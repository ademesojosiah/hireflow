package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.ScorecardCriterion;
import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.dto.request.ScorecardCriterionRequest;
import com.hireflow.hireflow.dto.response.ScorecardCriterionResponse;
import com.hireflow.hireflow.dto.response.ScorecardTemplateResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ScorecardTemplateMapper {

    public ScorecardCriterion toCriterion(ScorecardCriterionRequest request) {
        ScorecardCriterion criterion = new ScorecardCriterion();
        criterion.setCategory(request.getCategory());
        criterion.setName(request.getName());
        criterion.setDescription(request.getDescription());
        criterion.setMaxScore(request.getMaxScore());
        criterion.setDisplayOrder(request.getDisplayOrder());
        return criterion;
    }

    public ScorecardTemplateResponse toResponse(ScorecardTemplate template) {
        List<ScorecardCriterionResponse> criteria = template.getCriteria() == null ? List.of()
                : template.getCriteria().stream()
                .sorted(Comparator.comparing(ScorecardCriterion::getDisplayOrder))
                .map(this::toCriterionResponse)
                .toList();
        return new ScorecardTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.isActive(),
                criteria,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private ScorecardCriterionResponse toCriterionResponse(ScorecardCriterion criterion) {
        return new ScorecardCriterionResponse(
                criterion.getId(),
                criterion.getCategory(),
                criterion.getName(),
                criterion.getDescription(),
                criterion.getMaxScore(),
                criterion.getDisplayOrder()
        );
    }
}
