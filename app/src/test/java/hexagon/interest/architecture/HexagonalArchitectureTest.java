package hexagon.interest.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {
    private final JavaClasses applicationClasses = new ClassFileImporter().importPackages("hexagon.interest");

    @Test
    void domainDoesNotDependOnApplicationOrAdapters() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("hexagon.interest.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("hexagon.interest.application..", "hexagon.interest.adapter..");

        rule.check(applicationClasses);
    }

    @Test
    void applicationDoesNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("hexagon.interest.application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("hexagon.interest.adapter..");

        rule.check(applicationClasses);
    }

    @Test
    void applicationPortsDoNotDependOnServices() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("hexagon.interest.application.port..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("hexagon.interest.application.service..");

        rule.check(applicationClasses);
    }

    @Test
    void drivingAndDrivenPortsDoNotDependOnEachOther() {
        ArchRule drivingPortRule = noClasses()
                .that()
                .resideInAPackage("hexagon.interest.application.port.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("hexagon.interest.application.port.out..");
        ArchRule drivenPortRule = noClasses()
                .that()
                .resideInAPackage("hexagon.interest.application.port.out..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("hexagon.interest.application.port.in..");

        drivingPortRule.check(applicationClasses);
        drivenPortRule.check(applicationClasses);
    }
}
