package club.sk1er.patcher.asm

import club.sk1er.patcher.asm.utils.injectInstructions
import club.sk1er.patcher.tweaker.ClassTransformer
import club.sk1er.patcher.tweaker.transform.PatcherTransformer
import club.sk1er.patcher.util.hash.FastHashedKey
import codes.som.anthony.koffee.insns.jvm.aload_0
import codes.som.anthony.koffee.insns.jvm.getfield
import codes.som.anthony.koffee.insns.jvm.invokestatic
import org.objectweb.asm.tree.ClassNode

class ChunkCoordIntPairTransformer : PatcherTransformer {
    override fun getClassName() = arrayOf("net.minecraft.world.ChunkCoordIntPair")

    override fun transform(classNode: ClassNode, name: String) {
        if (ClassTransformer.optifineVersion != "NONE") {
            println("OptiFine detected, not optimizing ChunkCoordIntPair.")
            return
        } else {
            println("OptiFine not detected, optimizing ChunkCoordIntPair.")
        }

        classNode.methods.first {
            it.name == "hashCode"
        }?.apply {
            clearInstructions(this)
            injectInstructions {
                of(FastHashedKey::getFasterHashedKey)
                into(this@apply)
                param {
                    aload_0
                    getfield("net/minecraft/world/ChunkCoordIntPair", "field_77276_a", int)
                    aload_0
                    getfield("net/minecraft/world/ChunkCoordIntPair", "field_77275_b", int)
                    invokestatic("net/minecraft/world/ChunkCoordIntPair", "func_77272_a", long, int, int)
                }
            }
        }
    }
}