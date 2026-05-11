package com.gaminginsects.randomchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = RandomChunksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RandomChunks {

    private static final Set<String> VISITED_CHUNKS = new HashSet<>();
    private static List<Block> ALL_BLOCKS = null;

    public static boolean ENABLED = false;
    public static boolean RARE_BOOST = false;

    private static final List<Block> RARE_COMMON_BLOCKS = List.of(
            Blocks.DIAMOND_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.IRON_BLOCK,
            Blocks.RAW_IRON_BLOCK,
            Blocks.RAW_GOLD_BLOCK,
            Blocks.RAW_COPPER_BLOCK,
            Blocks.EMERALD_BLOCK
    );

    private static final List<Block> RARE_ULTRA_BLOCKS = List.of(
            Blocks.ANCIENT_DEBRIS,
            Blocks.NETHERITE_BLOCK
    );

    private static final Set<Block> BANNED_BLOCKS = Set.of(
            Blocks.AIR,
            Blocks.CAVE_AIR,
            Blocks.VOID_AIR,
            Blocks.STRUCTURE_VOID,
            Blocks.BARRIER,
            Blocks.LIGHT
    );

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;

        if (!ENABLED) return;

        Level level = event.player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos playerPos = event.player.blockPosition();
        ChunkPos chunkPos = new ChunkPos(playerPos);

        String dim = level.dimension().location().toString();
        String key = dim + ":" + chunkPos.x + "," + chunkPos.z;

        if (VISITED_CHUNKS.contains(key)) return;
        VISITED_CHUNKS.add(key);

        Block block = getRandomBlock(serverLevel, serverLevel.getRandom());
        randomizeWholeChunk(serverLevel, chunkPos, block);
    }

    private static void initBlockList(ServerLevel level) {
        if (ALL_BLOCKS != null) return;
        Registry<Block> registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        ALL_BLOCKS = registry.stream().toList();
    }

    private static Block getRandomBlock(ServerLevel level, RandomSource random) {
        initBlockList(level);
        BlockPos dummy = new BlockPos(0, 0, 0);

        if (RARE_BOOST && random.nextInt(4) == 0) {
            List<Block> pool = (random.nextInt(5) == 0 && !RARE_ULTRA_BLOCKS.isEmpty())
                    ? RARE_ULTRA_BLOCKS
                    : RARE_COMMON_BLOCKS;
            return pool.get(random.nextInt(pool.size()));
        }

        while (true) {
            Block candidate = ALL_BLOCKS.get(random.nextInt(ALL_BLOCKS.size()));

            if (BANNED_BLOCKS.contains(candidate)) continue;
            if (candidate.defaultBlockState().is(BlockTags.LEAVES)) continue;

            if (candidate instanceof FallingBlock) continue;
            if (candidate == Blocks.SAND ||
                candidate == Blocks.RED_SAND ||
                candidate == Blocks.GRAVEL ||
                candidate == Blocks.SUSPICIOUS_SAND ||
                candidate == Blocks.SUSPICIOUS_GRAVEL) {
                continue;
            }

            var state = candidate.defaultBlockState();
            boolean isLiquid = !state.getFluidState().isEmpty();
            boolean isFullSolid = state.isCollisionShapeFullBlock(level, dummy);

            boolean isSupport =
                    state.is(BlockTags.STAIRS) ||
                    state.is(BlockTags.WALLS) ||
                    state.is(BlockTags.FENCES) ||
                    state.is(BlockTags.FENCE_GATES) ||
                    state.is(Blocks.RAW_IRON_BLOCK) ||
                    state.is(Blocks.RAW_GOLD_BLOCK) ||
                    state.is(Blocks.RAW_COPPER_BLOCK);

            if (!isLiquid && !isFullSolid && !isSupport) continue;

            if (candidate == Blocks.WATER && random.nextInt(5) != 0) continue;

            return candidate;
        }
    }

    private static void randomizeWholeChunk(ServerLevel level, ChunkPos chunkPos, Block block) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        if (block instanceof FallingBlock) {
            block = Blocks.STONE;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    var state = level.getBlockState(pos);

                    if (state.isAir() ||
                        state.is(Blocks.CAVE_AIR) ||
                        state.is(Blocks.VOID_AIR)) continue;

                    if (state.is(Blocks.BEDROCK)) continue;
                    if (state.is(Blocks.LAVA)) continue;
                    if (state.is(Blocks.SPAWNER)) continue;

                    if (state.is(block)) continue;

                    level.setBlock(pos, block.defaultBlockState(), 2);
                }
            }
        }

        if (block == Blocks.WATER) {
            placeConduit(level, chunkPos);
        }
    }

    private static void placeConduit(ServerLevel level, ChunkPos chunkPos) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int cx = chunkPos.getMinBlockX() + 8;
        int cz = chunkPos.getMinBlockZ() + 8;

        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(cx, y, cz);
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlock(pos, Blocks.CONDUIT.defaultBlockState(), 2);
                break;
            }
        }
    }
}
