/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.reflect;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.module.ModuleDescriptor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.loader.BootLoader;
import jdk.internal.module.Modules;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.loader.ClassLoaderValue;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

import static java.lang.module.ModuleDescriptor.Modifier.SYNTHETIC;


/**
 *
 * {@code Proxy} provides static methods for creating objects that act like instances
 * of interfaces but allow for customized method invocation.
 * To create a proxy instance for some interface {@code Foo}:
 * <pre>{@code
 *     InvocationHandler handler = new MyInvocationHandler(...);
 *     Foo f = (Foo) Proxy.newProxyInstance(Foo.class.getClassLoader(),
 *                                          new Class<?>[] { Foo.class },
 *                                          handler);
 * }</pre>
 *
 * <p>
 * A <em>proxy class</em> is a class created at runtime that implements a specified
 * list of interfaces, known as <em>proxy interfaces</em>. A <em>proxy instance</em>
 * is an instance of a proxy class.
 *
 * Each proxy instance has an associated <i>invocation handler</i>
 * object, which implements the interface {@link InvocationHandler}.
 * A method invocation on a proxy instance through one of its proxy
 * interfaces will be dispatched to the {@link InvocationHandler#invoke
 * invoke} method of the instance's invocation handler, passing the proxy
 * instance, a {@code java.lang.reflect.Method} object identifying
 * the method that was invoked, and an array of type {@code Object}
 * containing the arguments.  The invocation handler processes the
 * encoded method invocation as appropriate and the result that it
 * returns will be returned as the result of the method invocation on
 * the proxy instance.
 *
 * <p>A proxy class has the following properties:
 *
 * <ul>
 * <li>The unqualified name of a proxy class is unspecified.  The space
 * of class names that begin with the string {@code "$Proxy"}
 * should be, however, reserved for proxy classes.
 *
 * <li>The package and module in which a proxy class is defined is specified
 * <a href="#membership">below</a>.
 *
 * <li>A proxy class is <em>final and non-abstract</em>.
 *
 * <li>A proxy class extends {@code java.lang.reflect.Proxy}.
 *
 * <li>A proxy class implements exactly the interfaces specified at its
 * creation, in the same order. Invoking {@link Class#getInterfaces getInterfaces}
 * on its {@code Class} object will return an array containing the same
 * list of interfaces (in the order specified at its creation), invoking
 * {@link Class#getMethods getMethods} on its {@code Class} object will return
 * an array of {@code Method} objects that include all of the
 * methods in those interfaces, and invoking {@code getMethod} will
 * find methods in the proxy interfaces as would be expected.
 *
 * <li>The {@link java.security.ProtectionDomain} of a proxy class
 * is the same as that of system classes loaded by the bootstrap class
 * loader, such as {@code java.lang.Object}, because the code for a
 * proxy class is generated by trusted system code.  This protection
 * domain will typically be granted {@code java.security.AllPermission}.
 *
 * <li>The {@link Proxy#isProxyClass Proxy.isProxyClass} method can be used
 * to determine if a given class is a proxy class.
 * </ul>
 *
 * <p>A proxy instance has the following properties:
 *
 * <ul>
 * <li>Given a proxy instance {@code proxy} and one of the
 * interfaces, {@code Foo}, implemented by its proxy class, the
 * following expression will return true:
 * <pre>
 *     {@code proxy instanceof Foo}
 * </pre>
 * and the following cast operation will succeed (rather than throwing
 * a {@code ClassCastException}):
 * <pre>
 *     {@code (Foo) proxy}
 * </pre>
 *
 * <li>Each proxy instance has an associated invocation handler, the one
 * that was passed to its constructor.  The static
 * {@link Proxy#getInvocationHandler Proxy.getInvocationHandler} method
 * will return the invocation handler associated with the proxy instance
 * passed as its argument.
 *
 * <li>An interface method invocation on a proxy instance will be
 * encoded and dispatched to the invocation handler's {@link
 * InvocationHandler#invoke invoke} method as described in the
 * documentation for that method.
 *
 * <li>An invocation of the {@code hashCode},
 * {@code equals}, or {@code toString} methods declared in
 * {@code java.lang.Object} on a proxy instance will be encoded and
 * dispatched to the invocation handler's {@code invoke} method in
 * the same manner as interface method invocations are encoded and
 * dispatched, as described above.  The declaring class of the
 * {@code Method} object passed to {@code invoke} will be
 * {@code java.lang.Object}.  Other public methods of a proxy
 * instance inherited from {@code java.lang.Object} are not
 * overridden by a proxy class, so invocations of those methods behave
 * like they do for instances of {@code java.lang.Object}.
 * </ul>
 *
 * <h3><a id="membership">Package and Module Membership of Proxy Class</a></h3>
 *
 * The package and module to which a proxy class belongs are chosen such that
 * the accessibility of the proxy class is in line with the accessibility of
 * the proxy interfaces. Specifically, the package and the module membership
 * of a proxy class defined via the
 * {@link Proxy#getProxyClass(ClassLoader, Class[])} or
 * {@link Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}
 * methods is specified as follows:
 *
 * <ol>
 * <li>If all the proxy interfaces are in <em>exported</em> or <em>open</em>
 *     packages:
 * <ol type="a">
 * <li>if all the proxy interfaces are <em>public</em>, then the proxy class is
 *     <em>public</em> in a package exported by the
 *     {@linkplain ClassLoader#getUnnamedModule() unnamed module} of the specified
 *     loader. The name of the package is unspecified.</li>
 *
 * <li>if at least one of all the proxy interfaces is <em>non-public</em>, then
 *     the proxy class is <em>non-public</em> in the package and module of the
 *     non-public interfaces. All the non-public interfaces must be in the same
 *     package and module; otherwise, proxying them is
 *     <a href="#restrictions">not possible</a>.</li>
 * </ol>
 * </li>
 * <li>If at least one proxy interface is in a package that is
 *     <em>non-exported</em> and <em>non-open</em>:
 * <ol type="a">
 * <li>if all the proxy interfaces are <em>public</em>, then the proxy class is
 *     <em>public</em> in a <em>non-exported</em>, <em>non-open</em> package of
 *     <a href="#dynamicmodule"><em>dynamic module</em>.</a>
 *     The names of the package and the module are unspecified.</li>
 *
 * <li>if at least one of all the proxy interfaces is <em>non-public</em>, then
 *     the proxy class is <em>non-public</em> in the package and module of the
 *     non-public interfaces. All the non-public interfaces must be in the same
 *     package and module; otherwise, proxying them is
 *     <a href="#restrictions">not possible</a>.</li>
 * </ol>
 * </li>
 * </ol>
 *
 * <p>
 * Note that if proxy interfaces with a mix of accessibilities -- for example,
 * an exported public interface and a non-exported non-public interface -- are
 * proxied by the same instance, then the proxy class's accessibility is
 * governed by the least accessible proxy interface.
 * <p>
 * Note that it is possible for arbitrary code to obtain access to a proxy class
 * in an open package with {@link AccessibleObject#setAccessible setAccessible},
 * whereas a proxy class in a non-open package is never accessible to
 * code outside the module of the proxy class.
 *
 * <p>
 * Throughout this specification, a "non-exported package" refers to a package
 * that is not exported to all modules, and a "non-open package" refers to
 * a package that is not open to all modules.  Specifically, these terms refer to
 * a package that either is not exported/open by its containing module or is
 * exported/open in a qualified fashion by its containing module.
 *
 * <h3><a id="dynamicmodule">Dynamic Modules</a></h3>
 * <p>
 * A dynamic module is a named module generated at runtime. A proxy class
 * defined in a dynamic module is encapsulated and not accessible to any module.
 * Calling {@link Constructor#newInstance(Object...)} on a proxy class in
 * a dynamic module will throw {@code IllegalAccessException};
 * {@code Proxy.newProxyInstance} method should be used instead.
 *
 * <p>
 * A dynamic module can read the modules of all of the superinterfaces of a proxy
 * class and the modules of the types referenced by all public method signatures
 * of a proxy class.  If a superinterface or a referenced type, say {@code T},
 * is in a non-exported package, the {@linkplain Module module} of {@code T} is
 * updated to export the package of {@code T} to the dynamic module.
 *
 * <h3>Methods Duplicated in Multiple Proxy Interfaces</h3>
 *
 * <p>When two or more proxy interfaces contain a method with
 * the same name and parameter signature, the order of the proxy class's
 * interfaces becomes significant.  When such a <i>duplicate method</i>
 * is invoked on a proxy instance, the {@code Method} object passed
 * to the invocation handler will not necessarily be the one whose
 * declaring class is assignable from the reference type of the interface
 * that the proxy's method was invoked through.  This limitation exists
 * because the corresponding method implementation in the generated proxy
 * class cannot determine which interface it was invoked through.
 * Therefore, when a duplicate method is invoked on a proxy instance,
 * the {@code Method} object for the method in the foremost interface
 * that contains the method (either directly or inherited through a
 * superinterface) in the proxy class's list of interfaces is passed to
 * the invocation handler's {@code invoke} method, regardless of the
 * reference type through which the method invocation occurred.
 *
 * <p>If a proxy interface contains a method with the same name and
 * parameter signature as the {@code hashCode}, {@code equals},
 * or {@code toString} methods of {@code java.lang.Object},
 * when such a method is invoked on a proxy instance, the
 * {@code Method} object passed to the invocation handler will have
 * {@code java.lang.Object} as its declaring class.  In other words,
 * the public, non-final methods of {@code java.lang.Object}
 * logically precede all of the proxy interfaces for the determination of
 * which {@code Method} object to pass to the invocation handler.
 *
 * <p>Note also that when a duplicate method is dispatched to an
 * invocation handler, the {@code invoke} method may only throw
 * checked exception types that are assignable to one of the exception
 * types in the {@code throws} clause of the method in <i>all</i> of
 * the proxy interfaces that it can be invoked through.  If the
 * {@code invoke} method throws a checked exception that is not
 * assignable to any of the exception types declared by the method in one
 * of the proxy interfaces that it can be invoked through, then an
 * unchecked {@code UndeclaredThrowableException} will be thrown by
 * the invocation on the proxy instance.  This restriction means that not
 * all of the exception types returned by invoking
 * {@code getExceptionTypes} on the {@code Method} object
 * passed to the {@code invoke} method can necessarily be thrown
 * successfully by the {@code invoke} method.
 *
 * @author      Peter Jones
 * @see         InvocationHandler
 * @since       1.3
 * @revised 9
 * @spec JPMS
 */
@NullMarked
public class Proxy implements java.io.Serializable {
    private static final long serialVersionUID = -2222568056686623797L;

    /** parameter types of a proxy class constructor */
    private static final Class<?>[] constructorParams =
        { InvocationHandler.class };

    /**
     * a cache of proxy constructors with
     * {@link Constructor#setAccessible(boolean) accessible} flag already set
     */
    private static final ClassLoaderValue<Constructor<?>> proxyCache =
        new ClassLoaderValue<>();

    /**
     * the invocation handler for this proxy instance.
     * @serial
     */
    protected InvocationHandler h;

    /**
     * Prohibits instantiation.
     */
    private Proxy() {
    }

    /**
     * Constructs a new {@code Proxy} instance from a subclass
     * (typically, a dynamic proxy class) with the specified value
     * for its invocation handler.
     *
     * @param  h the invocation handler for this proxy instance
     *
     * @throws NullPointerException if the given invocation handler, {@code h},
     *         is {@code null}.
     */
    protected Proxy(InvocationHandler h) {
        Objects.requireNonNull(h);
        this.h = h;
    }

    /**
     * Returns the {@code java.lang.Class} object for a proxy class
     * given a class loader and an array of interfaces.  The proxy class
     * will be defined by the specified class loader and will implement
     * all of the supplied interfaces.  If any of the given interfaces
     * is non-public, the proxy class will be non-public. If a proxy class
     * for the same permutation of interfaces has already been defined by the
     * class loader, then the existing proxy class will be returned; otherwise,
     * a proxy class for those interfaces will be generated dynamically
     * and defined by the class loader.
     *
     * @param   loader the class loader to define the proxy class
     * @param   interfaces the list of interfaces for the proxy class
     *          to implement
     * @return  a proxy class that is defined in the specified class loader
     *          and that implements the specified interfaces
     * @throws  IllegalArgumentException if any of the <a href="#restrictions">
     *          restrictions</a> on the parameters are violated
     * @throws  SecurityException if a security manager, <em>s</em>, is present
     *          and any of the following conditions is met:
     *          <ul>
     *             <li> the given {@code loader} is {@code null} and
     *             the caller's class loader is not {@code null} and the
     *             invocation of {@link SecurityManager#checkPermission
     *             s.checkPermission} with
     *             {@code RuntimePermission("getClassLoader")} permission
     *             denies access.</li>
     *             <li> for each proxy interface, {@code intf},
     *             the caller's class loader is not the same as or an
     *             ancestor of the class loader for {@code intf} and
     *             invocation of {@link SecurityManager#checkPackageAccess
     *             s.checkPackageAccess()} denies access to {@code intf}.</li>
     *          </ul>
     * @throws  NullPointerException if the {@code interfaces} array
     *          argument or any of its elements are {@code null}
     *
     * @deprecated Proxy classes generated in a named module are encapsulated
     *      and not accessible to code outside its module.
     *      {@link Constructor#newInstance(Object...) Constructor.newInstance}
     *      will throw {@code IllegalAccessException} when it is called on
     *      an inaccessible proxy class.
     *      Use {@link #newProxyInstance(ClassLoader, Class[], InvocationHandler)}
     *      to create a proxy instance instead.
     *
     * @see <a href="#membership">Package and Module Membership of Proxy Class</a>
     * @revised 9
     * @spec JPMS
     */
    @Deprecated
    @CallerSensitive
    public static Class<?> getProxyClass(@Nullable ClassLoader loader,
                                         Class<?>... interfaces)
        throws IllegalArgumentException
    {
        Class<?> caller = System.getSecurityManager() == null
                              ? null
                              : Reflection.getCallerClass();

        return getProxyConstructor(caller, loader, interfaces)
            .getDeclaringClass();
    }

    /**
     * Returns the {@code Constructor} object of a proxy class that takes a
     * single argument of type {@link InvocationHandler}, given a class loader
     * and an array of interfaces. The returned constructor will have the
     * {@link Constructor#setAccessible(boolean) accessible} flag already set.
     *
     * @param   caller passed from a public-facing @CallerSensitive method if
     *                 SecurityManager is set or {@code null} if there's no
     *                 SecurityManager
     * @param   loader the class loader to define the proxy class
     * @param   interfaces the list of interfaces for the proxy class
     *          to implement
     * @return  a Constructor of the proxy class taking single
     *          {@code InvocationHandler} parameter
     */
    private static Constructor<?> getProxyConstructor(@Nullable Class<?> caller,
                                                      ClassLoader loader,
                                                      Class<?>... interfaces)
    {
        // optimization for single interface
        if (interfaces.length == 1) {
            Class<?> intf = interfaces[0];
            if (caller != null) {
                checkProxyAccess(caller, loader, intf);
            }
            return proxyCache.sub(intf).computeIfAbsent(
                loader,
                (ld, clv) -> new ProxyBuilder(ld, clv.key()).build()
            );
        } else {
            // interfaces cloned
            final Class<?>[] intfsArray = interfaces.clone();
            if (caller != null) {
                checkProxyAccess(caller, loader, intfsArray);
            }
            final List<Class<?>> intfs = Arrays.asList(intfsArray);
            return proxyCache.sub(intfs).computeIfAbsent(
                loader,
                (ld, clv) -> new ProxyBuilder(ld, clv.key()).build()
            );
        }
    }

    /*
     * Check permissions required to create a Proxy class.
     *
     * To define a proxy class, it performs the access checks as in
     * Class.forName (VM will invoke ClassLoader.checkPackageAccess):
     * 1. "getClassLoader" permission check if loader == null
     * 2. checkPackageAccess on the interfaces it implements
     *
     * To get a constructor and new instance of a proxy class, it performs
     * the package access check on the interfaces it implements
     * as in Class.getConstructor.
     *
     * If an interface is non-public, the proxy class must be defined by
     * the defining loader of the interface.  If the caller's class loader
     * is not the same as the defining loader of the interface, the VM
     * will throw IllegalAccessError when the generated proxy class is
     * being defined.
     */
    private static void checkProxyAccess(Class<?> caller,
                                         ClassLoader loader,
                                         Class<?> ... interfaces)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader ccl = caller.getClassLoader();
            if (loader == null && ccl != null) {
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
            ReflectUtil.checkProxyPackageAccess(ccl, interfaces);
        }
    }

    /**
     * Builder for a proxy class.
     *
     * If the module is not specified in this ProxyBuilder constructor,
     * it will map from the given loader and interfaces to the module
     * in which the proxy class will be defined.
     */
    private static final class ProxyBuilder {
        private static final Unsafe UNSAFE = Unsafe.getUnsafe();

        // prefix for all proxy class names
        private static final String proxyClassNamePrefix = "$Proxy";

        // next number to use for generation of unique proxy class names
        private static final AtomicLong nextUniqueNumber = new AtomicLong();

        // a reverse cache of defined proxy classes
        private static final ClassLoaderValue<Boolean> reverseProxyCache =
            new ClassLoaderValue<>();

        private static Class<?> defineProxyClass(Module m, List<Class<?>> interfaces) {
            String proxyPkg = null;     // package to define proxy class in
            int accessFlags = Modifier.PUBLIC | Modifier.FINAL;

            /*
             * Record the package of a non-public proxy interface so that the
             * proxy class will be defined in the same package.  Verify that
             * all non-public proxy interfaces are in the same package.
             */
            for (Class<?> intf : interfaces) {
                int flags = intf.getModifiers();
                if (!Modifier.isPublic(flags)) {
                    accessFlags = Modifier.FINAL;  // non-public, final
                    String pkg = intf.getPackageName();
                    if (proxyPkg == null) {
                        proxyPkg = pkg;
                    } else if (!pkg.equals(proxyPkg)) {
                        throw new IllegalArgumentException(
                                "non-public interfaces from different packages");
                    }
                }
            }

            if (proxyPkg == null) {
                // all proxy interfaces are public
                proxyPkg = m.isNamed() ? PROXY_PACKAGE_PREFIX + "." + m.getName()
                                       : PROXY_PACKAGE_PREFIX;
            } else if (proxyPkg.isEmpty() && m.isNamed()) {
                throw new IllegalArgumentException(
                        "Unnamed package cannot be added to " + m);
            }

            if (m.isNamed()) {
                if (!m.getDescriptor().packages().contains(proxyPkg)) {
                    throw new InternalError(proxyPkg + " not exist in " + m.getName());
                }
            }

            /*
             * Choose a name for the proxy class to generate.
             */
            long num = nextUniqueNumber.getAndIncrement();
            String proxyName = proxyPkg.isEmpty()
                                    ? proxyClassNamePrefix + num
                                    : proxyPkg + "." + proxyClassNamePrefix + num;

            ClassLoader loader = getLoader(m);
            trace(proxyName, m, loader, interfaces);

            /*
             * Generate the specified proxy class.
             */
            byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
                    proxyName, interfaces.toArray(EMPTY_CLASS_ARRAY), accessFlags);
            try {
                Class<?> pc = UNSAFE.defineClass(proxyName, proxyClassFile,
                                                 0, proxyClassFile.length,
                                                 loader, null);
                reverseProxyCache.sub(pc).putIfAbsent(loader, Boolean.TRUE);
                return pc;
            } catch (ClassFormatError e) {
                /*
                 * A ClassFormatError here means that (barring bugs in the
                 * proxy class generation code) there was some other
                 * invalid aspect of the arguments supplied to the proxy
                 * class creation (such as virtual machine limitations
                 * exceeded).
                 */
                throw new IllegalArgumentException(e.toString());
            }
        }

        /**
         * Test if given class is a class defined by
         * {@link #defineProxyClass(Module, List)}
         */
        static boolean isProxyClass(Class<?> c) {
            return Objects.equals(reverseProxyCache.sub(c).get(c.getClassLoader()),
                                  Boolean.TRUE);
        }

        private static boolean isExportedType(Class<?> c) {
            String pn = c.getPackageName();
            return Modifier.isPublic(c.getModifiers()) && c.getModule().isExported(pn);
        }

        private static boolean isPackagePrivateType(Class<?> c) {
            return !Modifier.isPublic(c.getModifiers());
        }

        private static String toDetails(Class<?> c) {
            String access = "unknown";
            if (isExportedType(c)) {
                access = "exported";
            } else if (isPackagePrivateType(c)) {
                access = "package-private";
            } else {
                access = "module-private";
            }
            ClassLoader ld = c.getClassLoader();
            return String.format("   %s/%s %s loader %s",
                    c.getModule().getName(), c.getName(), access, ld);
        }

        static void trace(String cn,
                          Module module,
                          ClassLoader loader,
                          List<Class<?>> interfaces) {
            if (isDebug()) {
                System.err.format("PROXY: %s/%s defined by %s%n",
                                  module.getName(), cn, loader);
            }
            if (isDebug("debug")) {
                interfaces.forEach(c -> System.out.println(toDetails(c)));
            }
        }

        private static final String DEBUG =
            GetPropertyAction.privilegedGetProperty("jdk.proxy.debug", "");

        private static boolean isDebug() {
            return !DEBUG.isEmpty();
        }
        private static boolean isDebug(String flag) {
            return DEBUG.equals(flag);
        }

        // ProxyBuilder instance members start here....

        private final List<Class<?>> interfaces;
        private final Module module;
        ProxyBuilder(ClassLoader loader, List<Class<?>> interfaces) {
            if (!VM.isModuleSystemInited()) {
                throw new InternalError("Proxy is not supported until "
                        + "module system is fully initialized");
            }
            if (interfaces.size() > 65535) {
                throw new IllegalArgumentException("interface limit exceeded: "
                        + interfaces.size());
            }

            Set<Class<?>> refTypes = referencedTypes(loader, interfaces);

            // IAE if violates any restrictions specified in newProxyInstance
            validateProxyInterfaces(loader, interfaces, refTypes);

            this.interfaces = interfaces;
            this.module = mapToModule(loader, interfaces, refTypes);
            assert getLoader(module) == loader;
        }

        ProxyBuilder(ClassLoader loader, Class<?> intf) {
            this(loader, Collections.singletonList(intf));
        }

        /**
         * Generate a proxy class and return its proxy Constructor with
         * accessible flag already set. If the target module does not have access
         * to any interface types, IllegalAccessError will be thrown by the VM
         * at defineClass time.
         *
         * Must call the checkProxyAccess method to perform permission checks
         * before calling this.
         */
        Constructor<?> build() {
            Class<?> proxyClass = defineProxyClass(module, interfaces);
            final Constructor<?> cons;
            try {
                cons = proxyClass.getConstructor(constructorParams);
            } catch (NoSuchMethodException e) {
                throw new InternalError(e.toString(), e);
            }
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    cons.setAccessible(true);
                    return null;
                }
            });
            return cons;
        }

        /**
         * Validate the given proxy interfaces and the given referenced types
         * are visible to the defining loader.
         *
         * @throws IllegalArgumentException if it violates the restrictions
         *         specified in {@link Proxy#newProxyInstance}
         */
        private static void validateProxyInterfaces(ClassLoader loader,
                                                    List<Class<?>> interfaces,
                                                    Set<Class<?>> refTypes)
        {
            Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.size());
            for (Class<?> intf : interfaces) {
                /*
                 * Verify that the class loader resolves the name of this
                 * interface to the same Class object.
                 */
                ensureVisible(loader, intf);

                /*
                 * Verify that the Class object actually represents an
                 * interface.
                 */
                if (!intf.isInterface()) {
                    throw new IllegalArgumentException(intf.getName() + " is not an interface");
                }

                /*
                 * Verify that this interface is not a duplicate.
                 */
                if (interfaceSet.put(intf, Boolean.TRUE) != null) {
                    throw new IllegalArgumentException("repeated interface: " + intf.getName());
                }
            }

            for (Class<?> type : refTypes) {
                ensureVisible(loader, type);
            }
        }

        /*
         * Returns all types referenced by all public non-static method signatures of
         * the proxy interfaces
         */
        private static Set<Class<?>> referencedTypes(ClassLoader loader,
                                                     List<Class<?>> interfaces) {
            var types = new HashSet<Class<?>>();
            for (var intf : interfaces) {
                for (Method m : intf.getMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) {
                        addElementType(types, m.getReturnType());
                        addElementTypes(types, m.getSharedParameterTypes());
                        addElementTypes(types, m.getSharedExceptionTypes());
                    }
                }
            }
            return types;
        }

        private static void addElementTypes(HashSet<Class<?>> types,
                                            Class<?> ... classes) {
            for (var cls : classes) {
                addElementType(types, cls);
            }
        }

        private static void addElementType(HashSet<Class<?>> types,
                                           Class<?> cls) {
            var type = getElementType(cls);
            if (!type.isPrimitive()) {
                types.add(type);
            }
        }

        /**
         * Returns the module that the generated proxy class belongs to.
         *
         * If all proxy interfaces are public and in exported packages,
         * then the proxy class is in unnamed module.
         *
         * If any of proxy interface is package-private, then the proxy class
         * is in the same module of the package-private interface.
         *
         * If all proxy interfaces are public and at least one in a non-exported
         * package, then the proxy class is in a dynamic module in a
         * non-exported package.  Reads edge and qualified exports are added
         * for dynamic module to access.
         */
        private static Module mapToModule(ClassLoader loader,
                                          List<Class<?>> interfaces,
                                          Set<Class<?>> refTypes) {
            Map<Class<?>, Module> modulePrivateTypes = new HashMap<>();
            Map<Class<?>, Module> packagePrivateTypes = new HashMap<>();
            for (Class<?> intf : interfaces) {
                Module m = intf.getModule();
                if (Modifier.isPublic(intf.getModifiers())) {
                    // module-private types
                    if (!m.isExported(intf.getPackageName())) {
                        modulePrivateTypes.put(intf, m);
                    }
                } else {
                    packagePrivateTypes.put(intf, m);
                }
            }

            // all proxy interfaces are public and exported, the proxy class
            // is in unnamed module.  Such proxy class is accessible to
            // any unnamed module and named module that can read unnamed module
            if (packagePrivateTypes.isEmpty() && modulePrivateTypes.isEmpty()) {
                return loader != null ? loader.getUnnamedModule()
                                      : BootLoader.getUnnamedModule();
            }

            if (packagePrivateTypes.size() > 0) {
                // all package-private types must be in the same runtime package
                // i.e. same package name and same module (named or unnamed)
                //
                // Configuration will fail if M1 and in M2 defined by the same loader
                // and both have the same package p (so no need to check class loader)
                if (packagePrivateTypes.size() > 1 &&
                        (packagePrivateTypes.keySet().stream()  // more than one package
                                 .map(Class::getPackageName).distinct().count() > 1 ||
                         packagePrivateTypes.values().stream()  // or more than one module
                                 .distinct().count() > 1)) {
                    throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                }

                // all package-private types are in the same module (named or unnamed)
                Module target = null;
                for (Module m : packagePrivateTypes.values()) {
                    if (getLoader(m) != loader) {
                        // the specified loader is not the same class loader
                        // of the non-public interface
                        throw new IllegalArgumentException(
                                "non-public interface is not defined by the given loader");
                    }
                    target = m;
                }

                // validate if the target module can access all other interfaces
                for (Class<?> intf : interfaces) {
                    Module m = intf.getModule();
                    if (m == target) continue;

                    if (!target.canRead(m) || !m.isExported(intf.getPackageName(), target)) {
                        throw new IllegalArgumentException(target + " can't access " + intf.getName());
                    }
                }

                // return the module of the package-private interface
                return target;
            }

            // All proxy interfaces are public and at least one in a non-exported
            // package.  So maps to a dynamic proxy module and add reads edge
            // and qualified exports, if necessary
            Module target = getDynamicModule(loader);

            // set up proxy class access to proxy interfaces and types
            // referenced in the method signature
            Set<Class<?>> types = new HashSet<>(interfaces);
            types.addAll(refTypes);
            for (Class<?> c : types) {
                ensureAccess(target, c);
            }
            return target;
        }

        /*
         * Ensure the given module can access the given class.
         */
        private static void ensureAccess(Module target, Class<?> c) {
            Module m = c.getModule();
            // add read edge and qualified export for the target module to access
            if (!target.canRead(m)) {
                Modules.addReads(target, m);
            }
            String pn = c.getPackageName();
            if (!m.isExported(pn, target)) {
                Modules.addExports(m, pn, target);
            }
        }

        /*
         * Ensure the given class is visible to the class loader.
         */
        private static void ensureVisible(ClassLoader ld, Class<?> c) {
            Class<?> type = null;
            try {
                type = Class.forName(c.getName(), false, ld);
            } catch (ClassNotFoundException e) {
            }
            if (type != c) {
                throw new IllegalArgumentException(c.getName() +
                        " referenced from a method is not visible from class loader");
            }
        }

        private static Class<?> getElementType(Class<?> type) {
            Class<?> e = type;
            while (e.isArray()) {
                e = e.getComponentType();
            }
            return e;
        }

        private static final ClassLoaderValue<Module> dynProxyModules =
            new ClassLoaderValue<>();
        private static final AtomicInteger counter = new AtomicInteger();

        /*
         * Define a dynamic module for the generated proxy classes in
         * a non-exported package named com.sun.proxy.$MODULE.
         *
         * Each class loader will have one dynamic module.
         */
        private static Module getDynamicModule(ClassLoader loader) {
            return dynProxyModules.computeIfAbsent(loader, (ld, clv) -> {
                // create a dynamic module and setup module access
                String mn = "jdk.proxy" + counter.incrementAndGet();
                String pn = PROXY_PACKAGE_PREFIX + "." + mn;
                ModuleDescriptor descriptor =
                    ModuleDescriptor.newModule(mn, Set.of(SYNTHETIC))
                                    .packages(Set.of(pn))
                                    .build();
                Module m = Modules.defineModule(ld, descriptor, null);
                Modules.addReads(m, Proxy.class.getModule());
                // java.base to create proxy instance
                Modules.addExports(m, pn, Object.class.getModule());
                return m;
            });
        }
    }

    /**
     * Returns a proxy instance for the specified interfaces
     * that dispatches method invocations to the specified invocation
     * handler.
     * <p>
     * <a id="restrictions">{@code IllegalArgumentException} will be thrown
     * if any of the following restrictions is violated:</a>
     * <ul>
     * <li>All of {@code Class} objects in the given {@code interfaces} array
     * must represent interfaces, not classes or primitive types.
     *
     * <li>No two elements in the {@code interfaces} array may
     * refer to identical {@code Class} objects.
     *
     * <li>All of the interface types must be visible by name through the
     * specified class loader. In other words, for class loader
     * {@code cl} and every interface {@code i}, the following
     * expression must be true:<p>
     * {@code Class.forName(i.getName(), false, cl) == i}
     *
     * <li>All of the types referenced by all
     * public method signatures of the specified interfaces
     * and those inherited by their superinterfaces
     * must be visible by name through the specified class loader.
     *
     * <li>All non-public interfaces must be in the same package
     * and module, defined by the specified class loader and
     * the module of the non-public interfaces can access all of
     * the interface types; otherwise, it would not be possible for
     * the proxy class to implement all of the interfaces,
     * regardless of what package it is defined in.
     *
     * <li>For any set of member methods of the specified interfaces
     * that have the same signature:
     * <ul>
     * <li>If the return type of any of the methods is a primitive
     * type or void, then all of the methods must have that same
     * return type.
     * <li>Otherwise, one of the methods must have a return type that
     * is assignable to all of the return types of the rest of the
     * methods.
     * </ul>
     *
     * <li>The resulting proxy class must not exceed any limits imposed
     * on classes by the virtual machine.  For example, the VM may limit
     * the number of interfaces that a class may implement to 65535; in
     * that case, the size of the {@code interfaces} array must not
     * exceed 65535.
     * </ul>
     *
     * <p>Note that the order of the specified proxy interfaces is
     * significant: two requests for a proxy class with the same combination
     * of interfaces but in a different order will result in two distinct
     * proxy classes.
     *
     * @param   loader the class loader to define the proxy class
     * @param   interfaces the list of interfaces for the proxy class
     *          to implement
     * @param   h the invocation handler to dispatch method invocations to
     * @return  a proxy instance with the specified invocation handler of a
     *          proxy class that is defined by the specified class loader
     *          and that implements the specified interfaces
     * @throws  IllegalArgumentException if any of the <a href="#restrictions">
     *          restrictions</a> on the parameters are violated
     * @throws  SecurityException if a security manager, <em>s</em>, is present
     *          and any of the following conditions is met:
     *          <ul>
     *          <li> the given {@code loader} is {@code null} and
     *               the caller's class loader is not {@code null} and the
     *               invocation of {@link SecurityManager#checkPermission
     *               s.checkPermission} with
     *               {@code RuntimePermission("getClassLoader")} permission
     *               denies access;</li>
     *          <li> for each proxy interface, {@code intf},
     *               the caller's class loader is not the same as or an
     *               ancestor of the class loader for {@code intf} and
     *               invocation of {@link SecurityManager#checkPackageAccess
     *               s.checkPackageAccess()} denies access to {@code intf};</li>
     *          <li> any of the given proxy interfaces is non-public and the
     *               caller class is not in the same {@linkplain Package runtime package}
     *               as the non-public interface and the invocation of
     *               {@link SecurityManager#checkPermission s.checkPermission} with
     *               {@code ReflectPermission("newProxyInPackage.{package name}")}
     *               permission denies access.</li>
     *          </ul>
     * @throws  NullPointerException if the {@code interfaces} array
     *          argument or any of its elements are {@code null}, or
     *          if the invocation handler, {@code h}, is
     *          {@code null}
     *
     * @see <a href="#membership">Package and Module Membership of Proxy Class</a>
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public static Object newProxyInstance(@Nullable ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h) {
        Objects.requireNonNull(h);

        final Class<?> caller = System.getSecurityManager() == null
                                    ? null
                                    : Reflection.getCallerClass();

        /*
         * Look up or generate the designated proxy class and its constructor.
         */
        Constructor<?> cons = getProxyConstructor(caller, loader, interfaces);

        return newProxyInstance(caller, cons, h);
    }

    private static Object newProxyInstance(@Nullable Class<?> caller, // null if no SecurityManager
                                           Constructor<?> cons,
                                           InvocationHandler h) {
        /*
         * Invoke its constructor with the designated invocation handler.
         */
        try {
            if (caller != null) {
                checkNewProxyPermission(caller, cons.getDeclaringClass());
            }

            return cons.newInstance(new Object[]{h});
        } catch (IllegalAccessException | InstantiationException e) {
            throw new InternalError(e.toString(), e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new InternalError(t.toString(), t);
            }
        }
    }

    private static void checkNewProxyPermission(Class<?> caller, Class<?> proxyClass) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (ReflectUtil.isNonPublicProxyClass(proxyClass)) {
                ClassLoader ccl = caller.getClassLoader();
                ClassLoader pcl = proxyClass.getClassLoader();

                // do permission check if the caller is in a different runtime package
                // of the proxy class
                String pkg = proxyClass.getPackageName();
                String callerPkg = caller.getPackageName();

                if (pcl != ccl || !pkg.equals(callerPkg)) {
                    sm.checkPermission(new ReflectPermission("newProxyInPackage." + pkg));
                }
            }
        }
    }

    /**
     * Returns the class loader for the given module.
     */
    private static ClassLoader getLoader(Module m) {
        PrivilegedAction<ClassLoader> pa = m::getClassLoader;
        return AccessController.doPrivileged(pa);
    }

    /**
     * Returns true if the given class is a proxy class.
     *
     * @implNote The reliability of this method is important for the ability
     * to use it to make security decisions, so its implementation should
     * not just test if the class in question extends {@code Proxy}.
     *
     * @param   cl the class to test
     * @return  {@code true} if the class is a proxy class and
     *          {@code false} otherwise
     * @throws  NullPointerException if {@code cl} is {@code null}
     *
     * @revised 9
     * @spec JPMS
     */
    public static boolean isProxyClass(Class<?> cl) {
        return Proxy.class.isAssignableFrom(cl) && ProxyBuilder.isProxyClass(cl);
    }

    /**
     * Returns the invocation handler for the specified proxy instance.
     *
     * @param   proxy the proxy instance to return the invocation handler for
     * @return  the invocation handler for the proxy instance
     * @throws  IllegalArgumentException if the argument is not a
     *          proxy instance
     * @throws  SecurityException if a security manager, <em>s</em>, is present
     *          and the caller's class loader is not the same as or an
     *          ancestor of the class loader for the invocation handler
     *          and invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the invocation
     *          handler's class.
     */
    @CallerSensitive
    public static InvocationHandler getInvocationHandler(Object proxy)
        throws IllegalArgumentException
    {
        /*
         * Verify that the object is actually a proxy instance.
         */
        if (!isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }

        final Proxy p = (Proxy) proxy;
        final InvocationHandler ih = p.h;
        if (System.getSecurityManager() != null) {
            Class<?> ihClass = ih.getClass();
            Class<?> caller = Reflection.getCallerClass();
            if (ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(),
                                                    ihClass.getClassLoader()))
            {
                ReflectUtil.checkPackageAccess(ihClass);
            }
        }

        return ih;
    }

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final String PROXY_PACKAGE_PREFIX = ReflectUtil.PROXY_PACKAGE;
}
