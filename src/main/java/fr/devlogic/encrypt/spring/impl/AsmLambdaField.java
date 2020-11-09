package fr.devlogic.encrypt.spring.impl;

import fr.devlogic.encrypt.util.EncryptException;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

final class AsmLambdaField {

    private static final String LAMBDA_META_FACTORY_CLASS = "java/lang/invoke/LambdaMetafactory";
    private static final String LAMBDA_META_FACTORY_METHOD = "metafactory";
    private static final String TARGET_TYPE = "(Ljava/lang/String;)V";
    private static final String CLASS_INIT = "<clinit>";

    private AsmLambdaField() {
    }

    public static Map<String, List<Method>> retrieveLambdaStaticFields(Class<?>c) {
        List<Method> methods = new ArrayList<>();
        Map<String, List<Method>> fieldLambdas = new HashMap<>();

        String classFile = "/" + c.getName().replace(".", "/") + ".class";

        InputStream is = AsmLambdaField.class.getResourceAsStream(classFile);
        if (is == null) {
            throw new IllegalStateException("Cannot load " + c.getName());
        }

        ClassReader classReader;
        try {
            classReader = new ClassReader(is);
        } catch (IOException ex) {
            throw new EncryptException(ex);
        }

        classReader.accept(new ClassVisitor(Opcodes.ASM8) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (CLASS_INIT.equals(name)) {
                    return new MethodVisitor(Opcodes.ASM8) {


                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if ((Opcodes.PUTSTATIC == opcode) && !methods.isEmpty()) {
                                fieldLambdas.put(name, new ArrayList<>(methods));
                            }

                            methods.clear();
                            super.visitFieldInsn(opcode, owner, name, descriptor);
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {

                            String handleClass = bootstrapMethodHandle.getOwner();
                            String handleMethod = bootstrapMethodHandle.getName();

                            if (LAMBDA_META_FACTORY_CLASS.equals(handleClass) && LAMBDA_META_FACTORY_METHOD.equals(handleMethod)
                                   && (bootstrapMethodArguments.length == 3) && (bootstrapMethodArguments[1] instanceof Handle)) {
                                Handle handle = (Handle) bootstrapMethodArguments[1];
                                String targetType = handle.getDesc();

                                if (TARGET_TYPE.equals(targetType)) {
                                    String targetClassName = handle.getOwner();
                                    String targetMethod = handle.getName();
                                    try {
                                        Class<?> targetClass = Class.forName(targetClassName.replace('/', '.'));
                                        Method method = targetClass.getMethod(targetMethod, String.class);
                                        methods.add(method);
                                    } catch (ClassNotFoundException | NoSuchMethodException ex) {
                                        throw new EncryptException(ex);
                                    }
                                }
                            }

                            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                        }

                    };
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
           }
        }, 0);

        try {
            is.close();
        } catch (IOException ex) {
            throw new EncryptException(ex);
        }

        return fieldLambdas;
    }
}
