package fr.devlogic.encrypt.spring.impl;

import fr.devlogic.encrypt.spring.EncryptedProperty;
import fr.devlogic.encrypt.util.Encrypt;
import fr.devlogic.encrypt.util.EncryptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

final class EncryptedPropertyBeanProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(EncryptedPropertyBeanProcessor.class);

    private final Environment environment;
    private final Set<Class<? extends Annotation>> annotations;
    private final Map<Class<?>, Map<Method, Annotation>> encryptedFields;
    private static final String ALL_PROFILES = "*";
    private int numProcessedClasses = 0;

    public EncryptedPropertyBeanProcessor(Environment environment) {
        this.environment = environment;

        annotations = new HashSet<>();
        annotations.add(EncryptedProperty.class);

        encryptedFields = new HashMap<>();
    }

    private void processClasses() {
        List<String> allLoadedClasses = EncryptedPropertyAgent.getAllLoadedClasses();
        if (allLoadedClasses.size() == numProcessedClasses) {
            return;
        }

        // A copy is made to avoid concurrent access
        List<String> classToProcess = new ArrayList<>(allLoadedClasses.subList(numProcessedClasses, allLoadedClasses.size()));

        Set<Class> configurations = new HashSet<>();
        classToProcess.forEach(classeName -> {
            try {
                Class<?> c = Class.forName(classeName);
                if (Annotation.class.isAssignableFrom(c) && c.getAnnotation(EncryptedProperty.class) != null) {
                    annotations.add((Class<? extends Annotation>) c);
                }

                if (c.getAnnotation(Configuration.class) != null) {
                    configurations.add(c);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            }
        });

        if (!configurations.isEmpty()) {
            lookForStaticFieldsWithEncryptedAnnotation(configurations, encryptedFields);
        }

        numProcessedClasses = allLoadedClasses.size();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = ClassUtils.getUserClass(bean.getClass()); // remonte à la classe, évite de pointer sur un proxy

        processClasses(); // looking for new loaded classes

        Map<Method, Annotation> methodAnnotationMap = encryptedFields.get(beanClass);
        if (methodAnnotationMap != null) {
            methodAnnotationMap.forEach((m, a) -> unencryptField(bean, beanClass, m, null, a));
        }

        ConfigurationProperties configurationPropertiesAnnotation = beanClass.getAnnotation(ConfigurationProperties.class);
        Configuration configurationAnnotation = beanClass.getAnnotation(Configuration.class);

        if ((configurationPropertiesAnnotation != null) || (configurationAnnotation != null)) {
            EncryptedProperty annotationOnClass = beanClass.getAnnotation(EncryptedProperty.class);
            Arrays.stream(beanClass.getDeclaredMethods())
                    .filter(m -> annotations.stream().map(m::getAnnotation).anyMatch(Objects::nonNull))
                    .filter(m -> m.getName().startsWith("set"))
                    .forEach(setMethod -> unencryptField(bean, beanClass, setMethod, annotationOnClass, null));
        }
        return bean;
    }

    private void unencryptField(Object bean, Class<?> beanClass, Method setMethod, @Nullable EncryptedProperty annotationOnClass, @Nullable Annotation annotation) {
        String getMethodName = "get" + setMethod.getName().substring(3);
        try {
            Method getMethod = beanClass.getMethod(getMethodName);
            String value = (String) getMethod.invoke(bean);
            if (StringUtils.isEmpty(value)) {
                return;
            }

            if (annotation == null) {
                annotation = annotations.stream().map(setMethod::getAnnotation).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
            }

            String algo;
            String key;
            String profiles = null;

            if (!(annotation instanceof EncryptedProperty)) {
                try {
                    Method profilesMethod = annotation.annotationType().getMethod("profiles");
                    profiles = (String) profilesMethod.invoke(annotation);
                    profiles = profiles.trim();
                } catch (NoSuchMethodException ex) {
                    // nothing to do
                }

                annotation = annotation.annotationType().getAnnotation(EncryptedProperty.class);
            }

            EncryptedProperty annotationOnMethod = (EncryptedProperty) annotation;
            algo = annotationOnMethod.algo();
            key = annotationOnMethod.key();
            if (StringUtils.isEmpty(profiles)) {
                profiles = annotationOnMethod.profiles();
            }

            if (StringUtils.isEmpty(profiles) && annotationOnClass != null) {
                profiles = annotationOnClass.profiles();
            }

            profiles = profiles.trim();

            if (StringUtils.hasLength(profiles)) {
                boolean enabledProfils = false;
                boolean disabledProfils = false;

                String[] splittedProfiles = profiles.split(",");
                List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
                for (String profile : splittedProfiles) {

                    profile = profile.trim();
                    if (StringUtils.isEmpty(profile)) {
                        continue;
                    }

                    boolean rejection = false;
                    if (profile.startsWith("!")) {
                        rejection = true;
                        profile = profile.substring(1).trim();
                        if (StringUtils.isEmpty(profile)) {
                            continue;
                        }
                    }

                    enabledProfils |= ALL_PROFILES.equals(profile) && !rejection;
                    disabledProfils |= ALL_PROFILES.equals(profile) && rejection;

                    boolean contains = activeProfiles.contains(profile);
                    if (contains) {
                        enabledProfils |= !rejection;
                        disabledProfils |= rejection;
                    }
                }

                if (!enabledProfils || disabledProfils) {
                    return;
                }
            }

            if (StringUtils.isEmpty(algo) && annotationOnClass != null) {
                algo = annotationOnClass.algo();
            }

            if (StringUtils.isEmpty(key) && annotationOnClass != null) {
                key = annotationOnClass.key();
            }

            if (StringUtils.hasLength(key) && StringUtils.hasLength(algo)) {
                log.debug("Unencrypt {} for method {}.{}", value, beanClass.getName(), setMethod.getName());
                value = Encrypt.decrypt(value, algo, key);
                setMethod.invoke(bean, value);
            } else {
                throw new EncryptedPropertyBeanProcessorException("algo " + algo + " ou key " + key + " est vide");
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | GeneralSecurityException e) {
            throw new EncryptedPropertyBeanProcessorException("unencrypt", e);
        }
    }

    private Map<Class<?>, Map<Method, Annotation>> lookForStaticFieldsWithEncryptedAnnotation(Set<Class> allClasses, Map<Class<?>, Map<Method, Annotation>> staticEncryptedFields) {

        Set<Field> allFields = allClasses.stream().map(Class::getDeclaredFields).map(Arrays::asList).flatMap(List::stream).collect(Collectors.toSet());
        List<Field> annotatedFields = allFields.stream().filter(f -> annotations.stream().map(f::getAnnotation).anyMatch(Objects::nonNull)).collect(Collectors.toList());

        List<Field> biconsumerFields = annotatedFields.stream()
                .filter(f -> BiConsumer.class.isAssignableFrom(f.getType()))
                .filter(f -> (f.getModifiers() & Modifier.STATIC) != 0)
                .filter(f -> String.class.isAssignableFrom((Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1]))
                .collect(Collectors.toList());

        Set<? extends Class<?>> classesWithBiconsumerFields = biconsumerFields.stream().map(Field::getDeclaringClass).collect(Collectors.toSet());

        Map<? extends Class<?>, Map<String, List<Method>>> lambdaFields = classesWithBiconsumerFields.stream().collect(Collectors.toMap(c -> c, AsmLambdaField::retrieveLambdaStaticFields));

        biconsumerFields.forEach(f -> {
            Class<?> targetClass = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
            if (targetClass.getAnnotation(Configuration.class) == null && targetClass.getAnnotation(ConfigurationProperties.class) == null) {
                return;
            }
            Annotation annotation = annotations.stream().map(f::getAnnotation).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
            String fieldName = f.getName();
            Class<?> declaringClass = f.getDeclaringClass();
            Map<String, List<Method>> fieldNameLambdas = lambdaFields.get(declaringClass);
            if (fieldNameLambdas == null) {
                throw new EncryptException("Field " + fieldName + "does not have class container");
            }
            List<Method> methods = fieldNameLambdas.get(fieldName);
            if (methods == null) {
                throw new EncryptException("Field " + fieldName + "does not have lambdas");
            }
            methods.forEach(m -> staticEncryptedFields.computeIfAbsent(targetClass, c -> new HashMap<>()).put(m, annotation));
        });

        return staticEncryptedFields;
    }

    // NOSONAR
    public static final class EncryptedPropertyBeanProcessorException extends BeansException {

        public EncryptedPropertyBeanProcessorException(String msg) {
            super(msg);
        }

        public EncryptedPropertyBeanProcessorException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
