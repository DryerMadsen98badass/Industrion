package net.mads.industron.validation.rules;

import net.mads.industron.validation.IndustronValidation;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Enforces the architecture boundary for pure domain code. Concrete structure/block
 * adapters are intentionally outside this check; they are allowed to integrate with Create.
 */
public final class CreateDependencyBoundaryValidator implements ValidationRule {
    private static final byte[] CREATE_REFERENCE = "com/simibubi/create/".getBytes(StandardCharsets.ISO_8859_1);
    private static final List<String> PURE_DOMAIN_PREFIXES = List.of(
            "net/mads/industron/material/",
            "net/mads/industron/recipe/",
            "net/mads/industron/chemistry/"
    );
    private static final String STRUCTURE_ADAPTER_PREFIX = "net/mads/industron/material/structure/";

    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        try {
            URI location = IndustronValidation.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location).toAbsolutePath().normalize();
            if (Files.isDirectory(codePath)) {
                scanDirectory(codePath, diagnostics);
            } else if (Files.isRegularFile(codePath) && codePath.toString().endsWith(".jar")) {
                scanJar(codePath, diagnostics);
            }
        } catch (Exception exception) {
            diagnostics.warning(
                    ValidationSubsystem.INTEGRATION,
                    ValidationCode.VALIDATOR_FAILURE,
                    "create_dependency_boundary",
                    "Could not inspect compiled classes for Create dependencies: " + safeMessage(exception)
            );
        }
    }

    private static void scanDirectory(Path root, ValidationCollector diagnostics) throws IOException {
        List<Path> classes = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(classes::add);
        }
        classes.sort(Comparator.comparing(path -> normalize(root.relativize(path))));
        for (Path classFile : classes) {
            String relative = normalize(root.relativize(classFile));
            if (isPureDomainClass(relative) && contains(Files.readAllBytes(classFile), CREATE_REFERENCE)) {
                report(relative, diagnostics);
            }
        }
    }

    private static void scanJar(Path jarPath, ValidationCollector diagnostics) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            List<String> names = jar.stream()
                    .map(entry -> entry.getName())
                    .filter(name -> name.endsWith(".class"))
                    .filter(CreateDependencyBoundaryValidator::isPureDomainClass)
                    .sorted()
                    .toList();
            for (String name : names) {
                var entry = jar.getJarEntry(name);
                try (var input = jar.getInputStream(entry)) {
                    if (contains(input.readAllBytes(), CREATE_REFERENCE)) {
                        report(name, diagnostics);
                    }
                }
            }
        }
    }

    private static boolean isPureDomainClass(String className) {
        if (className.startsWith(STRUCTURE_ADAPTER_PREFIX)) return false;
        for (String prefix : PURE_DOMAIN_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }

    private static void report(String className, ValidationCollector diagnostics) {
        diagnostics.error(
                ValidationSubsystem.INTEGRATION,
                ValidationCode.INVALID_REFERENCE,
                className.replace('/', '.').replace(".class", ""),
                "Pure material/recipe/chemistry/assembly domain code directly references Create; use an integration adapter"
        );
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) return false;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
