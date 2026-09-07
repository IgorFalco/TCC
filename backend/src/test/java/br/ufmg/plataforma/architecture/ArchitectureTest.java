package br.ufmg.plataforma.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Travas de arquitetura (RNF-02) — verificam a regra de dependência da Seção 3.2 do plano:
 * um módulo só pode depender de módulos abaixo dele na ordem da Seção 3.2, mais {@code core}.
 */
@AnalyzeClasses(packages = "br.ufmg.plataforma", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** {@code iam} (M02) só pode depender de {@code core} entre os módulos da plataforma. */
    @ArchTest
    static final ArchRule iam_so_depende_de_core = noClasses()
            .that().resideInAPackage("br.ufmg.plataforma.iam..")
            .should().dependOnClassesThat(
                    resideInAPackage("br.ufmg.plataforma..")
                            .and(resideInAnyPackage(
                                    "br.ufmg.plataforma.iam..", "br.ufmg.plataforma.core..")
                                    .negate()));

    /** O domínio não conhece a camada web nem a de configuração. */
    @ArchTest
    static final ArchRule domain_nao_depende_de_web_ou_config = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..web..", "..config..");

    /** Sem dependências cíclicas entre os módulos de primeiro nível. */
    @ArchTest
    static final ArchRule modulos_livres_de_ciclos = slices()
            .matching("br.ufmg.plataforma.(*)..")
            .should().beFreeOfCycles();
}
