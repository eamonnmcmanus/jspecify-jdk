/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.security.AccessController;

import jdk.internal.misc.VM;
import jdk.internal.module.IllegalAccessLogger;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

/**
 * The {@code AccessibleObject} class is the base class for {@code Field},
 * {@code Method}, and {@code Constructor} objects (known as <em>reflected
 * objects</em>). It provides the ability to flag a reflected object as
 * suppressing checks for Java language access control when it is used. This
 * permits sophisticated applications with sufficient privilege, such as Java
 * Object Serialization or other persistence mechanisms, to manipulate objects
 * in a manner that would normally be prohibited.
 *
 * <p> Java language access control prevents use of private members outside
 * their top-level class; package access members outside their package; protected members
 * outside their package or subclasses; and public members outside their
 * module unless they are declared in an {@link Module#isExported(String,Module)
 * exported} package and the user {@link Module#canRead reads} their module. By
 * default, Java language access control is enforced (with one variation) when
 * {@code Field}s, {@code Method}s, or {@code Constructor}s are used to get or
 * set fields, to invoke methods, or to create and initialize new instances of
 * classes, respectively. Every reflected object checks that the code using it
 * is in an appropriate class, package, or module. </p>
 *
 * <p> The one variation from Java language access control is that the checks
 * by reflected objects assume readability. That is, the module containing
 * the use of a reflected object is assumed to read the module in which
 * the underlying field, method, or constructor is declared. </p>
 *
 * <p> Whether the checks for Java language access control can be suppressed
 * (and thus, whether access can be enabled) depends on whether the reflected
 * object corresponds to a member in an exported or open package
 * (see {@link #setAccessible(boolean)}). </p>
 *
 * @jls 6.6 Access Control
 * @since 1.2
 * @revised 9
 * @spec JPMS
 */
@NullMarked
public class AccessibleObject implements AnnotatedElement {

    static void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // SecurityConstants.ACCESS_PERMISSION is used to check
            // whether a client has sufficient privilege to defeat Java
            // language access control checks.
            sm.checkPermission(SecurityConstants.ACCESS_PERMISSION);
        }
    }

    /**
     * Convenience method to set the {@code accessible} flag for an
     * array of reflected objects with a single security check (for efficiency).
     *
     * <p> This method may be used to enable access to all reflected objects in
     * the array when access to each reflected object can be enabled as
     * specified by {@link #setAccessible(boolean) setAccessible(boolean)}. </p>
     *
     * <p>If there is a security manager, its
     * {@code checkPermission} method is first called with a
     * {@code ReflectPermission("suppressAccessChecks")} permission.
     *
     * <p>A {@code SecurityException} is also thrown if any of the elements of
     * the input {@code array} is a {@link java.lang.reflect.Constructor}
     * object for the class {@code java.lang.Class} and {@code flag} is true.
     *
     * @param array the array of AccessibleObjects
     * @param flag  the new value for the {@code accessible} flag
     *              in each object
     * @throws InaccessibleObjectException if access cannot be enabled for all
     *         objects in the array
     * @throws SecurityException if the request is denied by the security manager
     *         or an element in the array is a constructor for {@code
     *         java.lang.Class}
     * @see SecurityManager#checkPermission
     * @see ReflectPermission
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public static void setAccessible(AccessibleObject[] array, boolean flag) {
        checkPermission();
        if (flag) {
            Class<?> caller = Reflection.getCallerClass();
            array = array.clone();
            for (AccessibleObject ao : array) {
                ao.checkCanSetAccessible(caller);
            }
        }
        for (AccessibleObject ao : array) {
            ao.setAccessible0(flag);
        }
    }

    /**
     * Set the {@code accessible} flag for this reflected object to
     * the indicated boolean value.  A value of {@code true} indicates that
     * the reflected object should suppress checks for Java language access
     * control when it is used. A value of {@code false} indicates that
     * the reflected object should enforce checks for Java language access
     * control when it is used, with the variation noted in the class description.
     *
     * <p> This method may be used by a caller in class {@code C} to enable
     * access to a {@link Member member} of {@link Member#getDeclaringClass()
     * declaring class} {@code D} if any of the following hold: </p>
     *
     * <ul>
     *     <li> {@code C} and {@code D} are in the same module. </li>
     *
     *     <li> The member is {@code public} and {@code D} is {@code public} in
     *     a package that the module containing {@code D} {@link
     *     Module#isExported(String,Module) exports} to at least the module
     *     containing {@code C}. </li>
     *
     *     <li> The member is {@code protected} {@code static}, {@code D} is
     *     {@code public} in a package that the module containing {@code D}
     *     exports to at least the module containing {@code C}, and {@code C}
     *     is a subclass of {@code D}. </li>
     *
     *     <li> {@code D} is in a package that the module containing {@code D}
     *     {@link Module#isOpen(String,Module) opens} to at least the module
     *     containing {@code C}.
     *     All packages in unnamed and open modules are open to all modules and
     *     so this method always succeeds when {@code D} is in an unnamed or
     *     open module. </li>
     * </ul>
     *
     * <p> This method cannot be used to enable access to private members,
     * members with default (package) access, protected instance members, or
     * protected constructors when the declaring class is in a different module
     * to the caller and the package containing the declaring class is not open
     * to the caller's module. </p>
     *
     * <p> If there is a security manager, its
     * {@code checkPermission} method is first called with a
     * {@code ReflectPermission("suppressAccessChecks")} permission.
     *
     * @param flag the new value for the {@code accessible} flag
     * @throws InaccessibleObjectException if access cannot be enabled
     * @throws SecurityException if the request is denied by the security manager
     * @see #trySetAccessible
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive   // overrides in Method/Field/Constructor are @CS
    public void setAccessible(boolean flag) {
        AccessibleObject.checkPermission();
        setAccessible0(flag);
    }

    /**
     * Sets the accessible flag and returns the new value
     */
    boolean setAccessible0(boolean flag) {
        this.override = flag;
        return flag;
    }

    /**
     * Set the {@code accessible} flag for this reflected object to {@code true}
     * if possible. This method sets the {@code accessible} flag, as if by
     * invoking {@link #setAccessible(boolean) setAccessible(true)}, and returns
     * the possibly-updated value for the {@code accessible} flag. If access
     * cannot be enabled, i.e. the checks or Java language access control cannot
     * be suppressed, this method returns {@code false} (as opposed to {@code
     * setAccessible(true)} throwing {@code InaccessibleObjectException} when
     * it fails).
     *
     * <p> This method is a no-op if the {@code accessible} flag for
     * this reflected object is {@code true}.
     *
     * <p> For example, a caller can invoke {@code trySetAccessible}
     * on a {@code Method} object for a private instance method
     * {@code p.T::privateMethod} to suppress the checks for Java language access
     * control when the {@code Method} is invoked.
     * If {@code p.T} class is in a different module to the caller and
     * package {@code p} is open to at least the caller's module,
     * the code below successfully sets the {@code accessible} flag
     * to {@code true}.
     *
     * <pre>
     * {@code
     *     p.T obj = ....;  // instance of p.T
     *     :
     *     Method m = p.T.class.getDeclaredMethod("privateMethod");
     *     if (m.trySetAccessible()) {
     *         m.invoke(obj);
     *     } else {
     *         // package p is not opened to the caller to access private member of T
     *         ...
     *     }
     * }</pre>
     *
     * <p> If there is a security manager, its {@code checkPermission} method
     * is first called with a {@code ReflectPermission("suppressAccessChecks")}
     * permission. </p>
     *
     * @return {@code true} if the {@code accessible} flag is set to {@code true};
     *         {@code false} if access cannot be enabled.
     * @throws SecurityException if the request is denied by the security manager
     *
     * @since 9
     * @spec JPMS
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     */
    @CallerSensitive
    public final boolean trySetAccessible() {
        AccessibleObject.checkPermission();

        if (override == true) return true;

        // if it's not a Constructor, Method, Field then no access check
        if (!Member.class.isInstance(this)) {
            return setAccessible0(true);
        }

        // does not allow to suppress access check for Class's constructor
        Class<?> declaringClass = ((Member) this).getDeclaringClass();
        if (declaringClass == Class.class && this instanceof Constructor) {
            return false;
        }

        if (checkCanSetAccessible(Reflection.getCallerClass(),
                                  declaringClass,
                                  false)) {
            return setAccessible0(true);
        } else {
            return false;
        }
    }


   /**
    * If the given AccessibleObject is a {@code Constructor}, {@code Method}
    * or {@code Field} then checks that its declaring class is in a package
    * that can be accessed by the given caller of setAccessible.
    */
    void checkCanSetAccessible(Class<?> caller) {
        // do nothing, needs to be overridden by Constructor, Method, Field
    }

    final void checkCanSetAccessible(Class<?> caller, Class<?> declaringClass) {
        checkCanSetAccessible(caller, declaringClass, true);
    }

    private boolean checkCanSetAccessible(Class<?> caller,
                                          Class<?> declaringClass,
                                          boolean throwExceptionIfDenied) {
        if (caller == MethodHandle.class) {
            throw new IllegalCallerException();   // should not happen
        }

        Module callerModule = caller.getModule();
        Module declaringModule = declaringClass.getModule();

        if (callerModule == declaringModule) return true;
        if (callerModule == Object.class.getModule()) return true;
        if (!declaringModule.isNamed()) return true;

        String pn = declaringClass.getPackageName();
        int modifiers;
        if (this instanceof Executable) {
            modifiers = ((Executable) this).getModifiers();
        } else {
            modifiers = ((Field) this).getModifiers();
        }

        // class is public and package is exported to caller
        boolean isClassPublic = Modifier.isPublic(declaringClass.getModifiers());
        if (isClassPublic && declaringModule.isExported(pn, callerModule)) {
            // member is public
            if (Modifier.isPublic(modifiers)) {
                logIfExportedForIllegalAccess(caller, declaringClass);
                return true;
            }

            // member is protected-static
            if (Modifier.isProtected(modifiers)
                && Modifier.isStatic(modifiers)
                && isSubclassOf(caller, declaringClass)) {
                logIfExportedForIllegalAccess(caller, declaringClass);
                return true;
            }
        }

        // package is open to caller
        if (declaringModule.isOpen(pn, callerModule)) {
            logIfOpenedForIllegalAccess(caller, declaringClass);
            return true;
        }

        if (throwExceptionIfDenied) {
            // not accessible
            String msg = "Unable to make ";
            if (this instanceof Field)
                msg += "field ";
            msg += this + " accessible: " + declaringModule + " does not \"";
            if (isClassPublic && Modifier.isPublic(modifiers))
                msg += "exports";
            else
                msg += "opens";
            msg += " " + pn + "\" to " + callerModule;
            InaccessibleObjectException e = new InaccessibleObjectException(msg);
            if (printStackTraceWhenAccessFails()) {
                e.printStackTrace(System.err);
            }
            throw e;
        }
        return false;
    }

    private boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
        while (queryClass != null) {
            if (queryClass == ofClass) {
                return true;
            }
            queryClass = queryClass.getSuperclass();
        }
        return false;
    }

    private void logIfOpenedForIllegalAccess(Class<?> caller, Class<?> declaringClass) {
        Module callerModule = caller.getModule();
        Module targetModule = declaringClass.getModule();
        // callerModule is null during early startup
        if (callerModule != null && !callerModule.isNamed() && targetModule.isNamed()) {
            IllegalAccessLogger logger = IllegalAccessLogger.illegalAccessLogger();
            if (logger != null) {
                logger.logIfOpenedForIllegalAccess(caller, declaringClass, this::toShortString);
            }
        }
    }

    private void logIfExportedForIllegalAccess(Class<?> caller, Class<?> declaringClass) {
        Module callerModule = caller.getModule();
        Module targetModule = declaringClass.getModule();
        // callerModule is null during early startup
        if (callerModule != null && !callerModule.isNamed() && targetModule.isNamed()) {
            IllegalAccessLogger logger = IllegalAccessLogger.illegalAccessLogger();
            if (logger != null) {
                logger.logIfExportedForIllegalAccess(caller, declaringClass, this::toShortString);
            }
        }
    }

    /**
     * Returns a short descriptive string to describe this object in log messages.
     */
    String toShortString() {
        return toString();
    }

    /**
     * Get the value of the {@code accessible} flag for this reflected object.
     *
     * @return the value of the object's {@code accessible} flag
     *
     * @deprecated
     * This method is deprecated because its name hints that it checks
     * if the reflected object is accessible when it actually indicates
     * if the checks for Java language access control are suppressed.
     * This method may return {@code false} on a reflected object that is
     * accessible to the caller. To test if this reflected object is accessible,
     * it should use {@link #canAccess(Object)}.
     *
     * @revised 9
     * @spec JPMS
     */
    @Deprecated(since="9")
    public boolean isAccessible() {
        return override;
    }

    /**
     * Test if the caller can access this reflected object. If this reflected
     * object corresponds to an instance method or field then this method tests
     * if the caller can access the given {@code obj} with the reflected object.
     * For instance methods or fields then the {@code obj} argument must be an
     * instance of the {@link Member#getDeclaringClass() declaring class}. For
     * static members and constructors then {@code obj} must be {@code null}.
     *
     * <p> This method returns {@code true} if the {@code accessible} flag
     * is set to {@code true}, i.e. the checks for Java language access control
     * are suppressed, or if the caller can access the member as
     * specified in <cite>The Java&trade; Language Specification</cite>,
     * with the variation noted in the class description. </p>
     *
     * @param obj an instance object of the declaring class of this reflected
     *            object if it is an instance method or field
     *
     * @return {@code true} if the caller can access this reflected object.
     *
     * @throws IllegalArgumentException
     *         <ul>
     *         <li> if this reflected object is a static member or constructor and
     *              the given {@code obj} is non-{@code null}, or </li>
     *         <li> if this reflected object is an instance method or field
     *              and the given {@code obj} is {@code null} or of type
     *              that is not a subclass of the {@link Member#getDeclaringClass()
     *              declaring class} of the member.</li>
     *         </ul>
     *
     * @since 9
     * @spec JPMS
     * @jls 6.6 Access Control
     * @see #trySetAccessible
     * @see #setAccessible(boolean)
     */
    @CallerSensitive
    
    public final boolean canAccess(Object obj) {
        if (!Member.class.isInstance(this)) {
            return override;
        }

        Class<?> declaringClass = ((Member) this).getDeclaringClass();
        int modifiers = ((Member) this).getModifiers();
        if (!Modifier.isStatic(modifiers) &&
                (this instanceof Method || this instanceof Field)) {
            if (obj == null) {
                throw new IllegalArgumentException("null object for " + this);
            }
            // if this object is an instance member, the given object
            // must be a subclass of the declaring class of this reflected object
            if (!declaringClass.isAssignableFrom(obj.getClass())) {
                throw new IllegalArgumentException("object is not an instance of "
                                                   + declaringClass.getName());
            }
        } else if (obj != null) {
            throw new IllegalArgumentException("non-null object for " + this);
        }

        // access check is suppressed
        if (override) return true;

        Class<?> caller = Reflection.getCallerClass();
        Class<?> targetClass;
        if (this instanceof Constructor) {
            targetClass = declaringClass;
        } else {
            targetClass = Modifier.isStatic(modifiers) ? null : obj.getClass();
        }
        return verifyAccess(caller, declaringClass, targetClass, modifiers);
    }

    /**
     * Constructor: only used by the Java Virtual Machine.
     */
    protected AccessibleObject() {}

    // Indicates whether language-level access checks are overridden
    // by this object. Initializes to "false". This field is used by
    // Field, Method, and Constructor.
    //
    // NOTE: for security purposes, this field must not be visible
    // outside this package.
    boolean override;

    // Reflection factory used by subclasses for creating field,
    // method, and constructor accessors. Note that this is called
    // very early in the bootstrapping process.
    static final ReflectionFactory reflectionFactory =
        AccessController.doPrivileged(
            new ReflectionFactory.GetReflectionFactoryAction());

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <T extends Annotation> @Nullable T getAnnotation(Class<T> annotationClass) {
        throw new AssertionError("All subclasses should override this method");
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return AnnotatedElement.super.isAnnotationPresent(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        throw new AssertionError("All subclasses should override this method");
    }

    /**
     * @since 1.5
     */
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <T extends Annotation> @Nullable T getDeclaredAnnotation(Class<T> annotationClass) {
        // Only annotations on classes are inherited, for all other
        // objects getDeclaredAnnotation is the same as
        // getAnnotation.
        return getAnnotation(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        // Only annotations on classes are inherited, for all other
        // objects getDeclaredAnnotationsByType is the same as
        // getAnnotationsByType.
        return getAnnotationsByType(annotationClass);
    }

    /**
     * @since 1.5
     */
    public Annotation[] getDeclaredAnnotations()  {
        throw new AssertionError("All subclasses should override this method");
    }

    // Shared access checking logic.

    // For non-public members or members in package-private classes,
    // it is necessary to perform somewhat expensive security checks.
    // If the security check succeeds for a given class, it will
    // always succeed (it is not affected by the granting or revoking
    // of permissions); we speed up the check in the common case by
    // remembering the last Class for which the check succeeded.
    //
    // The simple security check for Constructor is to see if
    // the caller has already been seen, verified, and cached.
    // (See also Class.newInstance(), which uses a similar method.)
    //
    // A more complicated security check cache is needed for Method and Field
    // The cache can be either null (empty cache), a 2-array of {caller,targetClass},
    // or a caller (with targetClass implicitly equal to memberClass).
    // In the 2-array case, the targetClass is always different from the memberClass.
    volatile Object securityCheckCache;

    final void checkAccess(Class<?> caller, Class<?> memberClass,
                           Class<?> targetClass, int modifiers)
        throws IllegalAccessException
    {
        if (!verifyAccess(caller, memberClass, targetClass, modifiers)) {
            IllegalAccessException e = Reflection.newIllegalAccessException(
                caller, memberClass, targetClass, modifiers);
            if (printStackTraceWhenAccessFails()) {
                e.printStackTrace(System.err);
            }
            throw e;
        }
    }

    final boolean verifyAccess(Class<?> caller, Class<?> memberClass,
                               Class<?> targetClass, int modifiers)
    {
        if (caller == memberClass) {  // quick check
            return true;             // ACCESS IS OK
        }
        Object cache = securityCheckCache;  // read volatile
        if (targetClass != null // instance member or constructor
            && Modifier.isProtected(modifiers)
            && targetClass != memberClass) {
            // Must match a 2-list of { caller, targetClass }.
            if (cache instanceof Class[]) {
                Class<?>[] cache2 = (Class<?>[]) cache;
                if (cache2[1] == targetClass &&
                    cache2[0] == caller) {
                    return true;     // ACCESS IS OK
                }
                // (Test cache[1] first since range check for [1]
                // subsumes range check for [0].)
            }
        } else if (cache == caller) {
            // Non-protected case (or targetClass == memberClass or static member).
            return true;             // ACCESS IS OK
        }

        // If no return, fall through to the slow path.
        return slowVerifyAccess(caller, memberClass, targetClass, modifiers);
    }

    // Keep all this slow stuff out of line:
    private boolean slowVerifyAccess(Class<?> caller, Class<?> memberClass,
                                     Class<?> targetClass, int modifiers)
    {
        if (!Reflection.verifyMemberAccess(caller, memberClass, targetClass, modifiers)) {
            // access denied
            return false;
        }

        // access okay
        logIfExportedForIllegalAccess(caller, memberClass);

        // Success: Update the cache.
        Object cache = (targetClass != null
                        && Modifier.isProtected(modifiers)
                        && targetClass != memberClass)
                        ? new Class<?>[] { caller, targetClass }
                        : caller;

        // Note:  The two cache elements are not volatile,
        // but they are effectively final.  The Java memory model
        // guarantees that the initializing stores for the cache
        // elements will occur before the volatile write.
        securityCheckCache = cache;         // write volatile
        return true;
    }

    // true to print a stack trace when access fails
    private static volatile boolean printStackWhenAccessFails;

    // true if printStack* values are initialized
    private static volatile boolean printStackPropertiesSet;

    /**
     * Returns true if a stack trace should be printed when access fails.
     */
    private static boolean printStackTraceWhenAccessFails() {
        if (!printStackPropertiesSet && VM.initLevel() >= 1) {
            String s = GetPropertyAction.privilegedGetProperty(
                    "sun.reflect.debugModuleAccessChecks");
            if (s != null) {
                printStackWhenAccessFails = !s.equalsIgnoreCase("false");
            }
            printStackPropertiesSet = true;
        }
        return printStackWhenAccessFails;
    }

    /**
     * Returns the root AccessibleObject; or null if this object is the root.
     *
     * All subclasses override this method.
     */
    AccessibleObject getRoot() {
        throw new InternalError();
    }
}
