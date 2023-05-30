package fr.devlogic.encrypt.spring;

import fr.devlogic.encrypt.spring.impl.EncryptedPropertyAgent;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

class AgentTest {
    @Test
    void agent() throws ClassNotFoundException {
        Class.forName(ProprieteChiffree.class.getName()); // force class loading

        Set<Class> metaAnnotation = new ArrayList<>(EncryptedPropertyAgent.getAllLoadedClasses()).stream()
                .map(c -> {
                    try {
                        return Class.forName(c);
                    } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(Annotation.class::isAssignableFrom)
                .filter(c -> c.getAnnotation(EncryptedProperty.class) != null)
                .collect(Collectors.toSet());

        Assertions.assertThat(metaAnnotation).contains(ProprieteChiffree.class);
    }

    @Test
    void pid() {
        System.out.println("pid");
        ByteBuddyAgent.ProcessProvider.ForCurrentVm[] values = ByteBuddyAgent.ProcessProvider.ForCurrentVm.values();
        List<String> pids = Arrays.stream(values).map(ByteBuddyAgent.ProcessProvider.ForCurrentVm::resolve).collect(Collectors.toList());

        Assertions.assertThat(pids).isNotEmpty();
    }
}
