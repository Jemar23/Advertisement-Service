package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.concurrent.Callable;

public class SelectionLogicCallable implements Callable<TargetingPredicateResult> {
    private RequestContext requestContext;


    public SelectionLogicCallable(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public TargetingPredicateResult call() throws Exception {
        TargetingGroup targetingGroup = new TargetingGroup();
        Boolean grouping = targetingGroup.getTargetingPredicates().stream()
                .map(predicateResult -> predicateResult.evaluate(requestContext))
                .allMatch(predicateResult -> !predicateResult.isTrue() ? false : true);

        return grouping ? TargetingPredicateResult.TRUE : TargetingPredicateResult.FALSE;
    }
}
