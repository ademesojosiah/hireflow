package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.dao.SkillSearchDao;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.mapper.SkillMapper;
import com.hireflow.hireflow.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private static final int MIN_SEARCH_LENGTH = 3;

    private final SkillRepository skillRepository;
    private final SkillSearchDao skillSearchDao;
    private final SkillMapper skillMapper;

    @Override
    @Transactional
    public SkillResponse create(SkillRequest request) {
        try {
            if (request == null) {
                throw new CustomException("Skill request is required");
            }

            String name = normalizeName(request.getName());
            if (name.isBlank()) {
                throw new CustomException("Skill name is required");
            }
            request.setName(name);

            if (skillRepository.existsByNameIgnoreCase(name)) {
                throw new DuplicateResourceException("A skill with this name already exists");
            }

            Skill saved = skillRepository.save(skillMapper.toEntity(request));
            return skillMapper.toResponse(saved);
        } catch (CustomException | DuplicateResourceException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Skill creation failed: {}", ex.getMessage());
            throw new CustomException("Skill creation failed: Internal Server Error");
        }
    }

    @Override
    public List<SkillResponse> search(String query) {
        try {
            String prefix = normalizeName(query);
            if (prefix.length() < MIN_SEARCH_LENGTH) {
                throw new CustomException("Search query must be at least 3 characters");
            }

            return skillSearchDao.searchByNamePrefix(prefix);
        } catch (CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Skill search failed: {}", ex.getMessage());
            throw new CustomException("Skill search failed: Internal Server Error");
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
