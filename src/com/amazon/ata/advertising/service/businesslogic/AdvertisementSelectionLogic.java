package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.dao.TargetingGroupDao;
import com.amazon.ata.advertising.service.model.AdvertisementContent;
import com.amazon.ata.advertising.service.model.EmptyGeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.GeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final SortedMap<Double, AdvertisementContent> highestClickThroughRate;
    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
        this.highestClickThroughRate = new TreeMap<>();
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */

    // So now we want to only show ads that customers are eligible for, based on the customer being a part of an ad's targeting group.
    // For each loop -> content
    // get() from TargetingDao -> Optional

    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {
        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
        if (StringUtils.isEmpty(marketplaceId)) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        } else {
            List<AdvertisementContent> contents = contentDao.get(marketplaceId);

            if (CollectionUtils.isNotEmpty(contents)) {
                List<AdvertisementContent> advertisementContents = contents.stream()
                        .filter(advertisementContent -> targetingGroupDao.get(advertisementContent.getContentId())
                                .stream()
                                .anyMatch(targetingGroup -> {

                                    TargetingEvaluator evaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));
                                    TargetingPredicateResult predicateResult = null;

                                        try {
                                            predicateResult = evaluator.evaluate(targetingGroup);
                                        } catch (InterruptedException | ExecutionException e) {
                                            e.printStackTrace();
                                        }
                                    return predicateResult.isTrue();
                                }))
                        .collect(Collectors.toList());
                // Sort targeting group and return the highest click through rate
                // If user is included in multiple targeting groups -> compare the CTR and return the highest of the two
                // Descending map
                for (AdvertisementContent ad : advertisementContents) {
                    double currentHighestRate = 0;
                    List<TargetingGroup> targetingGroups = targetingGroupDao.get(ad.getContentId());
                    for (TargetingGroup group : targetingGroups) {
                        if (group.getClickThroughRate() > currentHighestRate) {
                            currentHighestRate = group.getClickThroughRate();
                        }
                    }
                    highestClickThroughRate.put(currentHighestRate, ad);
                }

                Double key = highestClickThroughRate.lastKey(); // Highest click through rate

                 AdvertisementContent highestAdvertisementContent = highestClickThroughRate.get(key);

                generatedAdvertisement = new GeneratedAdvertisement(highestAdvertisementContent);
            }

        }
        return generatedAdvertisement;
    }
}
//            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
//
//            if (CollectionUtils.isNotEmpty(contents)) {
//                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
//                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
//            }
//
//        }

//executorService.submit(() -> task.call().isTrue());
//                                    List<TargetingPredicateResult> ads = new ArrayList<>();