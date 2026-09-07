package br.ufmg.plataforma.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Travas de arquitetura (RNF-02) — verificam a regra de dependência da Seção 3.2 do plano.
 *
 * <p>Hoje só o pacote {@code core} tem código, então as regras passam trivialmente; ficam
 * prontas para quando os demais módulos forem adicionados.
 */
@AnalyzeClasses(packages = "br.ufmg.plataforma", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

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
