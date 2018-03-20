/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.engine.gregor.mutators;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.gregor.AbstractJumpMutator;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

enum OpcodeCompareToZero {
  IFEQ(Opcodes.IFEQ) {
    public String toString() {
      return "==";
    }
  },
  IFGE(Opcodes.IFGE) {
    public String toString() {
      return ">=";
    }
  },
  IFGT(Opcodes.IFGT) {
    public String toString() {
      return ">";
    }
  },
  IFLE(Opcodes.IFLE) {
    public String toString() {
      return "<=";
    }
  },
  IFLT(Opcodes.IFLT) {
    public String toString() {
      return "<";
    }
  },
  IFNE(Opcodes.IFNE) {
    public String toString() {
      return "!=";
    }
  };

  private final int opcode;

  OpcodeCompareToZero(int opcode) {
    this.opcode = opcode;
  }

  public int getOpcode() {
    return this.opcode;
  }
}

enum OpcodeCompare {
  IF_ICMPEQ(Opcodes.IF_ICMPEQ) {
    public String toString() {
      return "==";
    }
  },
  IF_ICMPGE(Opcodes.IF_ICMPGE) {
    public String toString() {
      return ">=";
    }
  },
  IF_ICMPGT(Opcodes.IF_ICMPGT) {
    public String toString() {
      return ">";
    }
  },
  IF_ICMPLE(Opcodes.IF_ICMPLE) {
    public String toString() {
      return "<=";
    }
  },
  IF_ICMPLT(Opcodes.IF_ICMPLT) {
    public String toString() {
      return "<";
    }
  },
  IF_ICMPNE(Opcodes.IF_ICMPNE) {
    public String toString() {
      return "!=";
    }
  };

  private final int opcode;

  OpcodeCompare(int opcode) {
    this.opcode = opcode;
  }

  public int getOpcode() {
    return this.opcode;
  }
}

public class RelationalOperatorReplacementMutator implements MethodMutatorFactory {

  // getName() is overridden, so the following line is not actually used.
  // RELATIONAL_OPERATOR_REPLACEMENT_MUTATOR;

  private final OpcodeCompareToZero operator;

  /**
   * Class constructor.
   *
   * @param operator to replace
   */
  public RelationalOperatorReplacementMutator(final OpcodeCompareToZero operator) {
    this.operator = operator;
  }

  /**
   * Create the mutator.
   *
   * @return MethodVisitor
   */
  @Override
  public MethodVisitor create(
      final MutationContext context,
      final MethodInfo methodInfo,
      final MethodVisitor methodVisitor) {
    switch (this.operator) {
    case IFEQ:
      return new RelationalOperatorReplacementIFEQMethodVisitor(
          this,
          context,
          methodVisitor);
    default:
      return null;
    }
  }

  @Override
  public String getGloballyUniqueId() {
    return this.getClass().getName();
  }

  @Override
  public String getName() {
    return "RELATIONAL_OPERATOR_" + this.operator.name()
        + "_REPLACEMENT_MUTATOR";
  }

}

class RelationalOperatorReplacementIFEQMethodVisitor extends AbstractJumpMutator {
  private static final Map<Integer, Substitution> MUTATIONS = new HashMap<>();
  private static final OpcodeCompareToZero REPLACEMENT_ZERO_OP
      = OpcodeCompareToZero.IFEQ;
  private static final OpcodeCompare REPLACEMENT_COMP_OP
      = OpcodeCompare.IF_ICMPEQ;

  static {
    // The operands will seem to be in the wrong order when used in
    // else conditions.  To the bytecode parser, though, this is not
    // the case.
    for (OpcodeCompareToZero original : OpcodeCompareToZero.values()) {
      if (REPLACEMENT_ZERO_OP != original) {
        MUTATIONS.put(
            original.getOpcode(),
            new Substitution(
                REPLACEMENT_ZERO_OP.getOpcode(),
                "Relational operator replacement: Mutated "
                    + original + " to " + REPLACEMENT_ZERO_OP));
      }
    }

    for (OpcodeCompare original : OpcodeCompare.values()) {
      if (REPLACEMENT_COMP_OP != original) {
        MUTATIONS.put(
            original.getOpcode(),
            new Substitution(
                REPLACEMENT_COMP_OP.getOpcode(),
                "Relational operator replacement: Mutated "
                    + original + " to " + REPLACEMENT_COMP_OP));
      }
    }
  }

  RelationalOperatorReplacementIFEQMethodVisitor(
      final MethodMutatorFactory factory,
      final MutationContext context,
      final MethodVisitor delegateMethodVisitor) {
    super(factory, context, delegateMethodVisitor);
  }

  @Override
  protected Map<Integer, Substitution> getMutations() {
    return MUTATIONS;
  }

}
