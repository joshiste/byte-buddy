package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AnnotationDrivenBinderTest {

    private static final String FOO = "foo", BAR = "bar", BAZ = "baz";

    private static class Key {

        private final String value;

        private Key(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && value.equals(((Key) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static @interface FirstPseudoAnnotation {
    }

    private static @interface SecondPseudoAnnotation {
    }

    private AnnotationDrivenBinder.ArgumentBinder<?> firstArgumentBinder, secondArgumentBinder;
    private AnnotationDrivenBinder.DefaultProvider<?> defaultProvider;
    private Assigner assigner;
    private Assignment assignment;
    private AnnotationDrivenBinder.MethodInvoker methodInvoker;
    private Assignment methodInvocation;

    private InstrumentedType0 typeDescription;
    private MethodDescription source, target;
    private TypeDescription sourceTypeDescription, targetTypeDescription;

    private FirstPseudoAnnotation firstPseudoAnnotation;
    private SecondPseudoAnnotation secondPseudoAnnotation;

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        firstArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        secondArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        defaultProvider = mock(AnnotationDrivenBinder.DefaultProvider.class);
        assigner = mock(Assigner.class);
        assignment = mock(Assignment.class);
        when(assignment.apply(any(MethodVisitor.class))).thenReturn(new Assignment.Size(0, 0));
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(assignment);
        methodInvoker = mock(AnnotationDrivenBinder.MethodInvoker.class);
        methodInvocation = mock(Assignment.class);
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(methodInvocation);
        when(methodInvocation.apply(any(MethodVisitor.class))).thenReturn(new Assignment.Size(0, 0));
        typeDescription = mock(InstrumentedType0.class);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(declaringType.isInterface()).thenReturn(false);
        when(target.getDeclaringType()).thenReturn(declaringType);
        when(target.getInternalName()).thenReturn(BAR);
        when(target.getDescriptor()).thenReturn(BAZ);
        when(target.isStatic()).thenReturn(true);
        firstPseudoAnnotation = mock(FirstPseudoAnnotation.class);
        doReturn(FirstPseudoAnnotation.class).when(firstPseudoAnnotation).annotationType();
        secondPseudoAnnotation = mock(SecondPseudoAnnotation.class);
        doReturn(SecondPseudoAnnotation.class).when(secondPseudoAnnotation).annotationType();
        methodVisitor = mock(MethodVisitor.class);
        sourceTypeDescription = mock(TypeDescription.class);
        when(sourceTypeDescription.getStackSize()).thenReturn(TypeSize.ZERO);
        targetTypeDescription = mock(TypeDescription.class);
        when(targetTypeDescription.getStackSize()).thenReturn(TypeSize.ZERO);
        when(source.getReturnType()).thenReturn(sourceTypeDescription);
        when(target.getReturnType()).thenReturn(targetTypeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingBinderBinding() throws Exception {
        doReturn(FirstPseudoAnnotation.class).when(firstArgumentBinder).getHandledType();
        doReturn(FirstPseudoAnnotation.class).when(secondArgumentBinder).getHandledType();
        new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner,
                methodInvoker);
    }

    @Test
    public void testDoNotBindAnnotation() throws Exception {
        IgnoreForBinding ignoreForBinding = mock(IgnoreForBinding.class);
        doReturn(IgnoreForBinding.class).when(ignoreForBinding).annotationType();
        when(target.getAnnotations()).thenReturn(new Annotation[]{ignoreForBinding});
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                defaultProvider,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(defaultProvider);
        verifyZeroInteractions(source);
    }

    @Test
    public void testReturnTypeMismatchNoRuntimeType() throws Exception {
        when(assignment.isValid()).thenReturn(false);
        when(methodInvocation.isValid()).thenReturn(true);
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                defaultProvider,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, false);
        verifyNoMoreInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verifyZeroInteractions(defaultProvider);
    }

    @Test
    public void testReturnTypeMismatchRuntimeType() throws Exception {
        when(assignment.isValid()).thenReturn(false);
        when(methodInvocation.isValid()).thenReturn(true);
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getAnnotations()).thenReturn(new Annotation[]{runtimeType});
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                defaultProvider,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, true);
        verifyNoMoreInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verifyZeroInteractions(defaultProvider);
    }

    @Test
    public void testNonAssignableMethodInvocation() throws Exception {
        fail("Implement test");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByDefaults() throws Exception {
        when(assignment.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider,
                Arrays.asList(secondPseudoAnnotation, firstPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner,
                methodInvoker);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, false);
        verifyNoMoreInteractions(assigner);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(2)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsufficientDefaults() throws Exception {
        when(assignment.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider,
                Arrays.asList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(1)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, false);
        verifyNoMoreInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
        verifyZeroInteractions(firstBinding);
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
        verify(secondBinding).getAssignment();
        verifyNoMoreInteractions(secondBinding);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotations() throws Exception {
        when(assignment.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{secondPseudoAnnotation}, {firstPseudoAnnotation}});
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider, Collections.<Annotation>emptyList());
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner,
                methodInvoker);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, false);
        verifyNoMoreInteractions(assigner);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verifyZeroInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotationsAndDefaults() throws Exception {
        when(assignment.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{}, {firstPseudoAnnotation}});
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider,
                Collections.singletonList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner,
                methodInvoker);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(targetTypeDescription, sourceTypeDescription, false);
        verifyNoMoreInteractions(assigner);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                typeDescription,
                assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator).hasNext();
        verify(defaultsIterator).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @SuppressWarnings("unchecked")
    private static AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> prepareArgumentBinder(AnnotationDrivenBinder.ArgumentBinder<?> argumentBinder,
                                                                                                    Class<? extends Annotation> annotationType,
                                                                                                    Object identificationToken,
                                                                                                    boolean bindingResult) {
        doReturn(annotationType).when(argumentBinder).getHandledType();
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = mock(AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.class);
        when(identifiedBinding.isValid()).thenReturn(bindingResult);
        when(identifiedBinding.getIdentificationToken()).thenReturn(identificationToken);
        when(identifiedBinding.getAssignment()).thenReturn(bindingResult ? LegalTrivialAssignment.INSTANCE : IllegalAssignment.INSTANCE);
        when(((AnnotationDrivenBinder.ArgumentBinder) argumentBinder).bind(any(Annotation.class),
                anyInt(),
                any(MethodDescription.class),
                any(MethodDescription.class),
                any(InstrumentedType0.class),
                any(Assigner.class)))
                .thenReturn(identifiedBinding);
        return identifiedBinding;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static Iterator<Annotation> prepareDefaultProvider(AnnotationDrivenBinder.DefaultProvider<?> defaultProvider,
                                                               List<? extends Annotation> defaultIteratorValues) {
        Iterator<Annotation> annotationIterator = mock(Iterator.class);
        when(defaultProvider.makeIterator(any(InstrumentedType0.class), any(MethodDescription.class), any(MethodDescription.class)))
                .thenReturn((Iterator) annotationIterator);
        OngoingStubbing<Boolean> iteratorConditionStubbing = when(annotationIterator.hasNext());
        for (Annotation defaultIteratorValue : defaultIteratorValues) {
            iteratorConditionStubbing = iteratorConditionStubbing.thenReturn(true);
        }
        iteratorConditionStubbing.thenReturn(false);
        OngoingStubbing<Annotation> iteratorValueStubbing = when(annotationIterator.next());
        for (Annotation defaultIteratorValue : defaultIteratorValues) {
            iteratorValueStubbing = iteratorValueStubbing.thenReturn(defaultIteratorValue);
        }
        iteratorValueStubbing.thenThrow(NoSuchElementException.class);
        return annotationIterator;
    }
}
