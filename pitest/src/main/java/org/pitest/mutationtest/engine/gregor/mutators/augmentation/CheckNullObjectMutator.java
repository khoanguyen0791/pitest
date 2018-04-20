package org.pitest.mutationtest.engine.gregor.mutators.augmentation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
//import org.pitest.mutationtest.engine.gregor.AbstractInsnMutator;
//import org.pitest.mutationtest.engine.gregor.InsnSubstitution;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
/*
 * M1 mutation
 */

/* M1 mutation, check for null before dereferencing.
 * 
 * field dereferencing are called using GETFIELD in java bytecode
 * Loading objects are loaded using ALOAD and ALOAD_n 
 * This code stops at GETFIELD and PUTFIELD. We skip ALOAD because 
 *  
 */
public enum CheckNullObjectMutator implements MethodMutatorFactory {

    CHECK_NULL_OBJECT_MUTATOR;

    /*
     * Create a MethodVisitor class to add null check (CheckNullObjectVisitor)
     * 
     * @see
     * org.pitest.mutationtest.engine.gregor.MethodMutatorFactory#create(org.pitest.
     * mutationtest.engine.gregor.MutationContext,
     * org.pitest.mutationtest.engine.gregor.MethodInfo,
     * org.objectweb.asm.MethodVisitor)
     */
    @Override
    public MethodVisitor create(final MutationContext context, final MethodInfo methodInfo, final MethodVisitor mv,
            ClassByteArraySource byteSource) {
        return new CheckNullObjectVisitor(this, methodInfo, context, mv);
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getClass().getName() + "_" + name();
    }

    @Override
    public String getName() {
        return "Check if object is null - " + name();
    }

}

/*
 * Two ways to do this. One is using Junit assertNotNull.Two is using IFNULL in
 * ASM.Extends ASM MethodVisitor to manipulate bytecode that goes into
 * MethodVisitor.
 */
class CheckNullObjectVisitor extends MethodVisitor {

    private final MethodMutatorFactory factory;
    private final MutationContext context;

    CheckNullObjectVisitor(final MethodMutatorFactory factory, final MethodInfo methodInfo,
            final MutationContext context, final MethodVisitor mv) {
        super(Opcodes.ASM6, mv);
        this.factory = factory;
        this.context = context;
    }

    /**
     * Override visitFieldInsn to check for PUTFIELD or GETFIELD If the bytecode is
     * PUTFIELD or GETFIELD, perform a nullcheck. opcode: Opcode instruction
     * GETFIELD, PUTFIELD owner: package name: method name that uses the variable
     * desc: variable type
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        final MutationIdentifier muID;
        if (opcode == Opcodes.GETFIELD) {
            muID = this.context.registerMutation(factory, "Checked for NULL object at GETFIELD.");
            if (this.context.shouldMutate(muID)) {
                mutateGetField(opcode, owner, name, desc);
            }

        } else if (opcode == Opcodes.PUTFIELD) {
            muID = this.context.registerMutation(factory, "Checked for NULL object at PUTFIELD.");
            if (this.context.shouldMutate(muID)) {
                mutatePutField(opcode, owner, name, desc);
            }
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    /**
     * There will be a 1-word objref at the top and 1-word objref below it
     * (instruction). Dup the top and analyze it.
     * 
     * @param opcode
     * @param owner
     * @param name
     * @param desc
     */
    public void mutateGetField(int opcode, String owner, String name, String desc) {
        super.visitInsn(Opcodes.DUP);
        Label ifNull = new Label();
        Label ifNotNull = new Label();
        Label returnToNormalCode = new Label();

        // visit ifNull if object is null
        super.visitJumpInsn(Opcodes.IFNONNULL, ifNotNull);

        // this happens if object is null, pop the duplicate then pop the method
        // instruction.
        super.visitLabel(ifNull);
        super.visitInsn(Opcodes.POP);
        super.visitInsn(Opcodes.POP);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);

        // this happens if object is not null, execute the instruction.
        super.visitLabel(ifNotNull);
        super.visitFieldInsn(opcode, owner, name, desc);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);

        super.visitLabel(returnToNormalCode);
    }

    /**
     * 
     * @param opcode
     * @param owner
     * @param name
     * @param desc
     */
    public void mutatePutField(int opcode, String owner, String name, String desc) {
        final Type type = Type.getType(desc);
        int size = type.getSize();
        if (size == 0 || size == 1) {
            oneWordVariable(opcode, owner, name, desc);
        } else if (size == 2) {
            twoWordVariable(opcode, owner, name, desc);
        }

        // int float boolean short char reference types = 1
        // long double 2
        // void 0
    }

    /**
     * Deals with the case there there's a two-word variable above the object
     * reference. If instruction is LDC2_W, use this one. The stack will look like
     * this: variable (can be 1 or 2-word) -> object reference (1 word)
     * 
     */
    public void twoWordVariable(int opcode, String owner, String name, String desc) {
        super.visitInsn(Opcodes.DUP2_X1);
        super.visitInsn(Opcodes.POP2);
        super.visitInsn(Opcodes.DUP);

        // create a label to mark where IFNULL jump to
        Label ifNull = new Label();
        Label ifNotNull = new Label();
        Label returnToNormalCode = new Label();

        // visit IFNONNULL if object is not null
        super.visitJumpInsn(Opcodes.IFNONNULL, ifNotNull);

        // this happens if object is null
        super.visitLabel(ifNull);
        super.visitInsn(Opcodes.POP);
        super.visitInsn(Opcodes.POP2);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);

        // this happens if object is not null
        super.visitLabel(ifNotNull);
        super.visitInsn(Opcodes.DUP_X2);
        super.visitInsn(Opcodes.POP);
        super.visitFieldInsn(opcode, owner, name, desc);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);
        super.visitLabel(returnToNormalCode);
    }

    /**
     * Deals with the case there there's a one-word variable above the object
     * reference. If instruction is LDC, use this one. The stack will look like
     * this: variable (can be 1 or 2-word) -> object reference (1 word)
     * 
     * 
     */
    public void oneWordVariable(int opcode, String owner, String name, String desc) {
        super.visitInsn(Opcodes.DUP_X1);
        super.visitInsn(Opcodes.POP);
        super.visitInsn(Opcodes.DUP);

        // create a label to mark where IFNULL jump to
        Label ifNull = new Label();
        Label ifNotNull = new Label();
        Label returnToNormalCode = new Label();

        // visit IFNULL if object is not null
        super.visitJumpInsn(Opcodes.IFNONNULL, ifNotNull);

        // this happens if object is null
        super.visitLabel(ifNull);
        super.visitInsn(Opcodes.POP);
        super.visitInsn(Opcodes.POP);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);

        // this happens if object is not null
        super.visitLabel(ifNotNull);
        super.visitInsn(Opcodes.SWAP);
        super.visitFieldInsn(opcode, owner, name, desc);
        super.visitJumpInsn(Opcodes.GOTO, returnToNormalCode);
        super.visitLabel(returnToNormalCode);
    }

    /**
     * We don't introduce any new variable, just one computation, so the maxStack
     * should not change. In FrameOptions.java, the ClassWriter already has
     * COMPUTE_FRAME so maybe this stack size will be automatically calculated.
     * 
     * @see org.objectweb.asm.MethodVisitor#visitMaxs(int, int)
     */
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
    }
}