package com.hireflow.hireflow.config;

import com.hireflow.hireflow.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillSeedCommandRunner implements CommandLineRunner {

    private static final List<String> DEFAULT_SKILLS = List.of(
            "Java",
            "Spring Boot",
            "JavaScript",
            "TypeScript",
            "React",
            "Angular",
            "Vue.js",
            "Node.js",
            "Python",
            "Django",
            "Flask",
            "PHP",
            "Laravel",
            "C#",
            ".NET",
            "Go",
            "Rust",
            "Kotlin",
            "Swift",
            "SQL",
            "MySQL",
            "PostgreSQL",
            "Database Design",
            "MongoDB",
            "Redis",
            "Elasticsearch",
            "Docker",
            "Kubernetes",
            "AWS",
            "Azure",
            "Google Cloud",
            "Git",
            "GitHub Actions",
            "CI/CD",
            "Jenkins",
            "Terraform",
            "Ansible",
            "Kafka",
            "RabbitMQ",
            "Microservices",
            "REST API",
            "GraphQL",
            "gRPC",
            "System Design",
            "Distributed Systems",
            "Clean Architecture",
            "Domain-Driven Design",
            "Unit Testing",
            "Integration Testing",
            "JUnit",
            "Mockito",
            "HTML",
            "CSS",
            "Tailwind CSS",
            "Bootstrap",
            "Sass",
            "Next.js",
            "Redux",
            "React Native",
            "Android",
            "iOS",
            "Figma",
            "UI/UX Design",
            "Product Design",
            "Agile",
            "Scrum",
            "Project Management",
            "Product Management",
            "Business Analysis",
            "Data Analysis",
            "Data Visualization",
            "Excel",
            "Power BI",
            "Tableau",
            "Machine Learning",
            "Deep Learning",
            "Natural Language Processing",
            "Computer Vision",
            "TensorFlow",
            "PyTorch",
            "Pandas",
            "NumPy",
            "R",
            "Data Engineering",
            "ETL",
            "Apache Spark",
            "Hadoop",
            "DevOps",
            "SRE",
            "Linux",
            "Bash",
            "PowerShell",
            "Networking",
            "Cybersecurity",
            "Penetration Testing",
            "OAuth",
            "JWT",
            "API Security",
            "Cloud Security",
            "Observability"
    );

    private static final int MINIMUM_SKILL_COUNT = DEFAULT_SKILLS.size();

    private final SkillService skillService;

    @Override
    public void run(String... args) {
        int seeded = skillService.seedDefaultsIfBelowMinimum(DEFAULT_SKILLS);
        if (seeded == 0) {
            log.info("Skill seeding skipped; at least {} skills already exist or defaults are present", MINIMUM_SKILL_COUNT);
            return;
        }

        log.info("Seeded {} default skills", seeded);
    }
}
