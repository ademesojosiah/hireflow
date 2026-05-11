package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.dao.SkillSearchDao;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.SkillMapper;
import com.hireflow.hireflow.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public List<Skill> findAllByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Skill> found = skillRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more skills not found");
        }
        return found;
    }

    @Override
    @Transactional
    public List<Skill> findOrCreateByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        List<String> normalizedNames = names.stream()
                .map(this::normalizeName)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        List<Skill> existingSkills = skillRepository.findAllByNameIgnoreCaseIn(normalizedNames);

        List<String> existingNames = existingSkills.stream()
                .map(skill -> normalizeName(skill.getName()))
                .toList();

        List<Skill> missingSkills = normalizedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Skill skill = new Skill();
                    skill.setName(name);
                    return skill;
                })
                .toList();

        if (!missingSkills.isEmpty()) {
            List<Skill> saved = skillRepository.saveAll(missingSkills);
            existingSkills.addAll(saved);
        }

        return existingSkills;
    }

    @Override
    @Transactional
    public int seedDefaultsIfBelowMinimum(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return 0;
        }

        if (skillRepository.count() >= skillNames.size()) {
            return 0;
        }

        List<Skill> missingSkills = skillNames.stream()
                .map(this::normalizeName)
                .filter(name -> !name.isBlank())
                .distinct()
                .filter(name -> !skillRepository.existsByNameIgnoreCase(name))
                .map(name -> {
                    Skill skill = new Skill();
                    skill.setName(name);
                    return skill;
                })
                .toList();

        if (missingSkills.isEmpty()) {
            return 0;
        }

        return skillRepository.saveAll(missingSkills).size();
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
