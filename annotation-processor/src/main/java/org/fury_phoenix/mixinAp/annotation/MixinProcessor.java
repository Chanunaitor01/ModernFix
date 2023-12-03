package org.fury_phoenix.mixinAp.annotation;

import com.google.auto.service.AutoService;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.fury_phoenix.mixinAp.config.MixinConfig;

@SupportedAnnotationTypes({"org.spongepowered.asm.mixin.Mixin", "org.embeddedt.modernfix.annotation.ClientOnlyMixin"})
@SupportedOptions({"rootProject.name", "project.name"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class MixinProcessor extends AbstractProcessor {

    // Remember to call toString when using aliases
    private static final Map<String, String> aliases = Map.of(
    "Mixin", "mixins",
    "ClientOnlyMixin", "client"
    );

    private final Map<String, List<String>> mixinConfigList = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()){
            filterMixinSets();
            // create record for serialization, compute package name
            String packageName = mixinConfigList.get("mixins").get(0).split("(?<=mixin)")[0];
            finalizeMixinConfig();
            new MixinConfig(packageName,
                mixinConfigList.get("mixins"),
                mixinConfigList.get("client")
            ).generateMixinConfig(processingEnv);
        } else {
            processMixins(annotations, roundEnv);
        }
        return false;
    }

    private void processMixins(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedMixins = roundEnv.getElementsAnnotatedWith(annotation);

            Stream<TypeElement> mixinStream =
            annotatedMixins.stream()
            .map(TypeElement.class::cast);

            validateCommonMixins(annotation, mixinStream);

            List<String> mixins =
            annotatedMixins.stream()
            .map(TypeElement.class::cast)
            .map(TypeElement::toString)
            .collect(Collectors.toList());

            mixinConfigList.putIfAbsent(aliases.get(annotation.getSimpleName().toString()), mixins);
        }
    }

    private void filterMixinSets() {
        List<String> commonSet = mixinConfigList.get("mixins");
        commonSet.removeAll(mixinConfigList.get("client"));
    }

    private void validateCommonMixins(TypeElement annotation, Stream<TypeElement> mixins) {
        if(!annotation.getSimpleName().toString().equals("Mixin"))
            return;
        ClientMixinValidator validator = new ClientMixinValidator(processingEnv);
        mixins.parallel()
        .filter(validator::targetsClient)
        .map(validator::getEntry)
        .forEach(this::logClientClassTarget);
    }

    private void logClientClassTarget(Map.Entry<? extends CharSequence, ? extends CharSequence> mixin) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
        "Mixin " + mixin.getKey() + " targets client-side classes: " + mixin.getValue());
    }

    private void finalizeMixinConfig() {
        // relativize class names
        for(var list : mixinConfigList.values()) {
            list.replaceAll(className -> className.split("(?<=mixin.)")[1]);
        }
    }
}
