package ru.ifmo.rain.lundin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static java.net.IDN.toUnicode;

public class Implementor implements JarImpler {

    /**
     * Class we want to implement
     */
    private Class<?> classname;

    /**
     * A set of implemented {@link Constructor}s and {@link Method}s of {@link Implementor#classname}
     */
    private Set<String> functions;

    /**
     * Creates a directory according to class package and path as a suffix
     *
     * @param className {@link Class} a class which package directories you want to create
     * @param path      {@link Path} path as a suffix to className's package
     * @return Path which has been created
     * @throws ImplerException if it can't create a directory
     */
    private Path createDirectory(Class className, Path path) throws ImplerException {
        Path path1 = path.resolve(className.getPackage().getName().replace('.', File.separatorChar))
                .resolve(className.getSimpleName() + "Impl.java");
        return createDirectory(path1);
    }

    /**
     * Creates a directory according to path given
     *
     * @param path file which {@link Path#getParent} is created
     * @return path itself
     * @throws ImplerException if it can't create a directory
     */
    private Path createDirectory(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Unable to create directories for output file", e);
            }

        }
        return path;
    }

    /**
     * Function returning a {@link StringBuilder} with header of a given class implementation
     *
     * @param className {@link Class} which header you want to get
     * @return {@link StringBuilder} header of given {@link Class}
     */
    private StringBuilder getHeader(Class className) {
        StringBuilder sb = new StringBuilder();
        if (!className.getPackage().getName().isEmpty()) {
            sb.append(className.getPackage()).append(";").append(System.lineSeparator());
        }
        sb.append("public class ")
                .append(className.getSimpleName())
                .append("Impl ");
        if (className.isInterface()) {
            sb.append("implements ")
                    .append(className.getSimpleName())
                    .append(" {");
        } else {
            sb.append("extends ")
                    .append(className.getSimpleName())
                    .append(" {")
                    .append(System.lineSeparator());
        }
        return sb;
    }

    /**
     * Generates a {@link StringBuilder} with a return type of a given executable
     *
     * @param exe {@link Executable} which return type you want to get
     * @return {@link StringBuilder} with return type of a given {@link Executable}
     */
    private StringBuilder getReturn(Executable exe) {
        if (exe instanceof Method) {
            return new StringBuilder(((Method) exe).getReturnType().getCanonicalName());
        }
        return new StringBuilder();
    }

    /**
     * Generates a {@link StringBuilder} with arguments of a given executable
     *
     * @param exe {@link Executable} which arguments you want to get
     * @return {@link StringBuilder} with arguments of a given {@link Executable}
     */
    private StringBuilder getArguments(Executable exe) {
        StringBuilder sb = new StringBuilder("(");
        int number = 0;
        for (Class<?> arg : exe.getParameterTypes()) {
            sb.append(arg.getCanonicalName())
                    .append(" ")
                    .append("arg")
                    .append(number)
                    .append(", ");
            ++number;
        }
        if (number != 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(")");
        return sb;
    }

    /**
     * Generates a {@link StringBuilder} with body of a give {@link Executable}
     *
     * @param exe {@link Executable} which method body you want to get
     * @return {@link StringBuilder} with body of a given {@link Executable}
     */
    private StringBuilder getMethodBody(Executable exe) {
        StringBuilder sb = new StringBuilder();
        if (exe instanceof Method) {
            Class retclass = ((Method) exe).getReturnType();
            if (retclass == void.class) {
                return sb;
            } else {
                if (retclass.isPrimitive()) {
                    if (retclass == boolean.class) {
                        sb.append("return false;")
                                .append(System.lineSeparator());
                    } else {
                        sb.append("return 0;")
                                .append(System.lineSeparator());
                    }
                } else {
                    sb.append("return null;")
                            .append(System.lineSeparator());
                }
            }
        } else if (exe instanceof Constructor) {
            sb.append(System.lineSeparator())
                    .append("super(");
            int len = exe.getParameterTypes().length;
            if (len != 0) {
                for (int i = 0; i < len; ++i) {
                    sb.append("arg")
                            .append(i)
                            .append(", ");
                }

                sb.setLength(sb.length() - 2);
            }
            sb.append(");")
                    .append(System.lineSeparator());

        }
        return sb;
    }

    /**
     * Generates a {@link StringBuilder} with getThrowing block of a give {@link Executable}
     *
     * @param exe {@link Executable} which getThrowing block you want to get
     * @return {@link StringBuilder} with getThrowing block of a given {@link Executable}
     */
    private StringBuilder getThrowing(Executable exe) {
        StringBuilder sb = new StringBuilder();
        if (exe.getExceptionTypes().length != 0) {
            sb.append(" throws ");
            for (Class cls : exe.getExceptionTypes()) {
                sb.append(cls.getCanonicalName()).append(", ");
            }

            sb.setLength(sb.length() - 2);
        }
        return sb;
    }

    /**
     * Implements given {@link Executable} exe and adds its signature and body to {@link Implementor#functions}
     *
     * @param exe {@link Executable} which you want to implement
     * @param foo {@link Function} predicate which you think a function should meet
     */
    private void implementMethod(Executable exe, Function<Integer, Boolean> foo) {
        StringBuilder funkCode = new StringBuilder();
        int mod = exe.getModifiers();
        if (!foo.apply(mod)) {
            return;
        }
        mod = mod & (Modifier.classModifiers() ^ Modifier.ABSTRACT);
        funkCode.append(Modifier.toString(mod))
                .append(" ")
                .append(getReturn(exe))
                .append(" ")
                .append((exe instanceof Method ? exe.getName() : classname.getSimpleName() + "Impl"))
                .append(" ")
                .append(getArguments(exe))
                .append(getThrowing(exe))
                .append("{")
                .append(System.lineSeparator())
                .append(getMethodBody(exe))
                .append("}")
                .append(System.lineSeparator());
        functions.add(funkCode.toString());
    }

    /**
     * Implements an array of {@link Executable}
     *
     * @param mas array of {@link Executable} which you want to implement
     * @param foo {@link Function} predicate which you think a function should meet
     */
    private void implement(Executable[] mas, Function<Integer, Boolean> foo) {
        for (Executable exe : mas) {
            implementMethod(exe, foo);
        }
    }

    /**
     * Implements all {@link Method}s of {@link Implementor#classname}
     * and all the {@link Constructor}s of {@link Implementor#classname} if it is not an Interface
     * Uses {@link Class#isInterface()} to determinate class from interface
     *
     * @throws ImplerException if {@link Implementor#classname} is Class but don't have any non-private constructors
     */
    private void implementMethods() throws ImplerException {
        if (!classname.isInterface()) {
            int num = functions.size();
            implement(classname.getDeclaredConstructors(), x -> !Modifier.isPrivate(x));
            if (num == functions.size()) {
                throw new ImplerException("No constructors in give class");
            }
        }
        implement(classname.getMethods(), Modifier::isAbstract);
        Class<?> classname1 = classname;
        while (classname1 != null) {
            implement(classname1.getDeclaredMethods(), Modifier::isAbstract);
            classname1 = classname1.getSuperclass();
        }
    }

    /**
     * Checks if the parameters given to our functions are valid
     *
     * @param className see {@link Implementor#implement(Class, Path)}
     * @param root      see {@link Implementor#implement(Class, Path)}
     * @throws ImplerException if the arguments were not applicable
     */
    private void checkArguments(Class className, Path root) throws ImplerException {
        if (className == null) {
            throw new ImplerException("class is null");
        } else if (root == null) {
            throw new ImplerException("path to class is null");
        } else if (className.isPrimitive() || className.isArray() || className == Enum.class || Modifier.isFinal(className.getModifiers())) {
            throw new ImplerException("Incorrect class type");
        }
    }

    /**
     * Implements a class
     * Output file is @param classname + Impl.java
     * Puts output file to @param <code>root</code> / @param <code>className</code> package
     * initialises {@link Implementor#functions} with empty {@link java.util.HashSet}
     * initialises {@link Implementor#classname} with @param className
     *
     * @param className a {@link Class} you want to implement
     * @param root      {@link Path} - a prefix in which generated .java file will be created
     * @throws ImplerException <ul>
     *                         <li>
     *                         if arguments provided wasn't valid
     *                         see {@link Implementor#checkArguments(Class, Path)}
     *                         </li>
     *                         <li>
     *                         if directory given can't be created
     *                         see {@link Implementor#createDirectory(Path)}
     *                         </li>
     *                         <li>
     *                         if {@link Class} given is not an Interface and doesn't nave non-private constructors
     *                         see {@link Implementor#implementMethods()}
     *                         </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> className, Path root) throws ImplerException {
        checkArguments(className, root);
        root = createDirectory(className, root);
        try (BufferedWriter outFile = Files.newBufferedWriter(root)) {
            StringBuilder code = new StringBuilder();
            this.classname = className;
            functions = new HashSet<>();
            code.append(getHeader(className));
            implementMethods();

            for (String func : functions) {
                code.append(func);
            }
            code.append("}");
            outFile.write(toUnicode(code.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Implements a class and puts its .class into .jar provided in @param root
     * Uses {@link Implementor#implement(Class, Path)} to implement {@link Class} className
     *
     * @param className a {@link Class} you want to implement
     * @param root      {@link Path} - path to .jar file you want a class implementation to be placed in
     * @throws ImplerException see {@link Implementor#implement(Class, Path)}
     *                         and
     *                         <ul>
     *                         <li>if {@link ToolProvider#getSystemJavaCompiler()} wasn't able to compile code generated by {@link Implementor#implement(Class, Path)} </li>
     *                         <li>if {@link Files#createTempDirectory(String, FileAttribute[]) wasn't able to create a directory}  </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> className, Path root) throws ImplerException {
        checkArguments(className, root);
        createDirectory(root);
        Manifest manifest = new Manifest();
        Path temp;

        try {
            temp = Files.createTempDirectory(root.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }
        Path file;
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0.0.0.0");
        try (JarOutputStream jstr = new JarOutputStream(Files.newOutputStream(root), manifest)) {
            implement(className, temp);
            file = createDirectory(className, temp);
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc == null) {
                throw new ImplerException("No system Java compiler");
            }
            String[] args = new String[]{
                    "-cp", temp.toString() + File.pathSeparator + System.getProperty("java.class.path"), file.toString(), "-encoding", "utf8"
            };

            if (jc.run(null, null, null, args) != 0) {
                throw new ImplerException("Can't compile generated class");
            }
            jstr.putNextEntry(new ZipEntry(className.getCanonicalName().replace('.', '/') + "Impl.class"));
            Files.copy(Paths.get(file.getParent().toString() + File.separatorChar + classname.getSimpleName() + "Impl.class"), jstr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main function of the program
     *
     * @param args ["-jar" "class_name" "jar"] if we want create jar from file created by
     *             ["class_name", "jar"] if we want create only java file
     *             class_name - name of input class
     *             jar - path to jar file.
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 3 && args.length != 2)) {
            System.err.println("Arguments provided are not applicable");
            return;
        }
        boolean jar = false;
        try {
            JarImpler imp = new Implementor();

            if (args.length == 2) {
                imp.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                jar = true;
                if (!args[0].equals("-jar")) {
                    System.err.println("-jar argument expected");
                }
                imp.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ImplerException | ClassNotFoundException e) {

            System.err.println("No such class " + args[(jar ? 1 : 0)]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path " + args[(jar ? 2 : 1)]);
        }
    }
}
