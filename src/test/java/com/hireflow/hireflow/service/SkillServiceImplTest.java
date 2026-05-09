package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.dao.SkillSearchDao;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.SkillMapper;
import com.hireflow.hireflow.service.impl.SkillServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock private SkillRepository skillRepository;
    @Mock private SkillSearchDao skillSearchDao;
    @Mock private SkillMapper skillMapper;
    @InjectMocks private SkillServiceImpl skillService;

    @Test
    @DisplayName("Should create a normalized skill when name is unique")
    void create_success() {
        SkillRequest request = new SkillRequest("  Java   Spring  ");
        Skill skill = new Skill();
        skill.setName("Java Spring");
        Skill saved = new Skill();
        saved.setId("skill-1");
        saved.setName("Java Spring");
        SkillResponse response = new SkillResponse("skill-1", "Java Spring");

        when(skillRepository.existsByNameIgnoreCase("Java Spring")).thenReturn(false);
        when(skillMapper.toEntity(request)).thenReturn(skill);
        when(skillRepository.save(skill)).thenReturn(saved);
        when(skillMapper.toResponse(saved)).thenReturn(response);

        SkillResponse result = skillService.create(request);

        assertThat(result.getName()).isEqualTo("Java Spring");
        assertThat(request.getName()).isEqualTo("Java Spring");
        verify(skillRepository).save(skill);
    }

    @Test
    @DisplayName("Should reject duplicate skill names case-insensitively")
    void create_duplicateName() {
        SkillRequest request = new SkillRequest("java");
        when(skillRepository.existsByNameIgnoreCase("java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject null skill request")
    void create_nullRequest() {
        assertThatThrownBy(() -> skillService.create(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Skill request is required");

        verifyNoInteractions(skillRepository, skillMapper);
    }

    @Test
    @DisplayName("Should reject blank skill name")
    void create_blankName() {
        SkillRequest request = new SkillRequest("   ");

        assertThatThrownBy(() -> skillService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Skill name is required");

        verifyNoInteractions(skillRepository, skillMapper);
    }

    @Test
    @DisplayName("Should wrap unexpected repository failure during skill creation")
    void create_repositoryFailure() {
        SkillRequest request = new SkillRequest("Scala");
        when(skillRepository.existsByNameIgnoreCase("Scala"))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> skillService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Skill creation failed");

        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should search skills by normalized three-character prefix")
    void search_success() {
        List<SkillResponse> matches = List.of(new SkillResponse("skill-1", "Java"));
        when(skillSearchDao.searchByNamePrefix("jav")).thenReturn(matches);

        List<SkillResponse> result = skillService.search("  jav  ");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Java");
        verify(skillSearchDao).searchByNamePrefix("jav");
    }

    @Test
    @DisplayName("Should collapse extra spaces before searching")
    void search_collapsesWhitespace() {
        when(skillSearchDao.searchByNamePrefix("data science")).thenReturn(List.of());

        skillService.search("  data    science  ");

        verify(skillSearchDao).searchByNamePrefix("data science");
    }

    @Test
    @DisplayName("Should reject skill searches below three characters")
    void search_tooShort() {
        assertThatThrownBy(() -> skillService.search("ja"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("at least 3 characters");

        verifyNoInteractions(skillSearchDao);
    }

    @Test
    @DisplayName("Should reject null skill search query")
    void search_nullQuery() {
        assertThatThrownBy(() -> skillService.search(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("at least 3 characters");

        verifyNoInteractions(skillSearchDao);
    }

    @Test
    @DisplayName("Should wrap unexpected DAO failure during skill search")
    void search_daoFailure() {
        when(skillSearchDao.searchByNamePrefix("java"))
                .thenThrow(new RuntimeException("query failed"));

        assertThatThrownBy(() -> skillService.search("java"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Skill search failed");

        verify(skillSearchDao).searchByNamePrefix("java");
    }

    @Test
    @DisplayName("Should return skills by ids when all exist")
    void findAllByIds_success() {
        Set<String> ids = Set.of("skill-1", "skill-2");
        Skill java = new Skill();
        java.setId("skill-1");
        Skill spring = new Skill();
        spring.setId("skill-2");
        when(skillRepository.findAllById(ids)).thenReturn(List.of(java, spring));

        List<Skill> result = skillService.findAllByIds(ids);

        assertThat(result).hasSize(2);
        verify(skillRepository).findAllById(ids);
    }

    @Test
    @DisplayName("Should throw when one or more skill ids are missing")
    void findAllByIds_missingSkill() {
        Set<String> ids = Set.of("skill-1", "skill-missing");
        Skill java = new Skill();
        java.setId("skill-1");
        when(skillRepository.findAllById(ids)).thenReturn(List.of(java));

        assertThatThrownBy(() -> skillService.findAllByIds(ids))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("skills not found");
    }

    @Test
    @DisplayName("Should seed default skills when stored count is below the seed list size")
    void seedDefaultsIfBelowMinimum_success() {
        List<String> defaults = List.of(" Java ", "Spring Boot", "Java");
        when(skillRepository.count()).thenReturn(1L);
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.existsByNameIgnoreCase("Spring Boot")).thenReturn(false);
        when(skillRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int seeded = skillService.seedDefaultsIfBelowMinimum(defaults);

        assertThat(seeded).isEqualTo(2);
        verify(skillRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip default skill seeding when stored count meets the seed list size")
    void seedDefaultsIfBelowMinimum_skipsWhenEnoughSkillsExist() {
        List<String> defaults = List.of("Java", "Spring Boot");
        when(skillRepository.count()).thenReturn(2L);

        int seeded = skillService.seedDefaultsIfBelowMinimum(defaults);

        assertThat(seeded).isZero();
        verify(skillRepository, never()).saveAll(anyList());
    }
}
