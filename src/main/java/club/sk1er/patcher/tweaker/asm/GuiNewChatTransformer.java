/*
 * Copyright © 2020 by Sk1er LLC
 *
 * All rights reserved.
 *
 * Sk1er LLC
 * 444 S Fulton Ave
 * Mount Vernon, NY
 * sk1er.club
 */

package club.sk1er.patcher.tweaker.asm;

import club.sk1er.patcher.tweaker.transform.CommonTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;
import java.util.ListIterator;

public class GuiNewChatTransformer implements CommonTransformer {
    /**
     * The class name that's being transformed
     *
     * @return the class name
     */
    @Override
    public String[] getClassName() {
        return new String[]{"net.minecraft.client.gui.GuiNewChat"};
    }

    /**
     * Perform any asm in order to transform code
     *
     * @param classNode the transformed class node
     * @param name      the transformed class name
     */
    @Override
    public void transform(ClassNode classNode, String name) {
        for (MethodNode methodNode : classNode.methods) {
            String methodName = mapMethodName(classNode, methodNode);

            switch (methodName) {
                case "setChatLine":
                case "func_146237_a": {
                    ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();

                    while (iterator.hasNext()) {
                        AbstractInsnNode node = iterator.next();

                        if (node instanceof IntInsnNode && ((IntInsnNode) node).operand == 100) {
                            methodNode.instructions.insertBefore(node, new IntInsnNode(Opcodes.SIPUSH, 10000));
                            methodNode.instructions.remove(node);
                        }
                    }
                    break;
                }

                case "drawChat":
                case "func_146230_a": {
                    Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
                    while (iterator.hasNext()) {
                        AbstractInsnNode node = iterator.next();
                        if (node.getOpcode() == Opcodes.INVOKESTATIC && node.getPrevious().getOpcode() == Opcodes.ISHL) {
                            LabelNode ifeq = new LabelNode();
                            methodNode.instructions.insert(node, ifeq);
                            AbstractInsnNode prevNode = node;

                            for (int i = 0; i < 15; i++) {
                                prevNode = prevNode.getPrevious();
                            }

                            methodNode.instructions.insertBefore(prevNode, new FieldInsnNode(Opcodes.GETSTATIC, getPatcherConfigClass(), "transparentChat", "Z"));
                            methodNode.instructions.insertBefore(prevNode, new JumpInsnNode(Opcodes.IFNE, ifeq));
                            break;
                        }
                    }
                    break;
                }

                case "getChatComponent":
                case "func_146236_a": {
                    this.changeChatComponentHeight(methodNode);
                    break;
                }
            }
        }
    }
}
