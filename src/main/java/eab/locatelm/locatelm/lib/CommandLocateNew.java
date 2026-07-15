package eab.locatelm.locatelm.lib;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import eab.structure_api.api.FeatureLocateHelper;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandLocateNew extends CommandBase {

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.locate.usage";
    }

    private static ITextComponent wrapInSquareBrackets(ITextComponent component) {
        return new TextComponentString("[").appendSibling(component).appendSibling(new TextComponentString("]"));
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 2) {
            throw new CommandException("commands.locate.usage", new Object[0]);
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String id = args[1].toLowerCase(Locale.ROOT);

        BlockPos senderPos = sender.getPosition();
        World world = sender.getEntityWorld();

        if (type.equals("structure")) {
            String vanillaStructureId = mapStructureIdToVanilla(id);
            
            BlockPos structurePos = world.findNearestStructure(vanillaStructureId, senderPos, false);

            if (structurePos == null) {
                throw new CommandException("commands.locate.fail", vanillaStructureId);
            } else {
                int x = structurePos.getX();
                int y = structurePos.getY();
                int z = structurePos.getZ();

                int distance = (int) Math.sqrt(senderPos.distanceSq(structurePos));

                ITextComponent coordinatesComponent = new TextComponentString(String.format(Locale.ROOT, "%d %d %d", x, y, z));
                coordinatesComponent.setStyle(new Style()
                    .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " " + y + " " + z))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("chat.coordinates.tooltip")))
                    .setColor(TextFormatting.GREEN));

                ITextComponent wrappedCoordinates = wrapInSquareBrackets(coordinatesComponent);

                ITextComponent structureNameComponent = new TextComponentTranslation("structure." + vanillaStructureId);
                ITextComponent distanceComponent = new TextComponentString(String.valueOf(distance));

                ITextComponent finalMessage = new TextComponentTranslation(
                    "commands.locate.success",
                    structureNameComponent,
                    wrappedCoordinates,
                    distanceComponent
                );

                sender.sendMessage(finalMessage);
            }
        } else if (type.equals("biome")) {
            locateBiome(server, sender, id);
        } else if (type.equals("feature")) {
            if (id.equals("dungeon")) {
                BlockPos featurePos = FeatureLocateHelper.findFeature(world, "dungeon", senderPos);

                if (featurePos == null) {
                    throw new CommandException("commands.locate.fail", "Dungeon");
                } else {
                    int x = featurePos.getX();
                    int y = featurePos.getY();
                    int z = featurePos.getZ();
                    int distance = (int) Math.sqrt(senderPos.distanceSq(featurePos));

                    ITextComponent coordinatesComponent = new TextComponentString(String.format(Locale.ROOT, "%d %d %d", x, y, z));
                    coordinatesComponent.setStyle(new Style()
                        .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " " + y + " " + z))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("chat.coordinates.tooltip")))
                        .setColor(TextFormatting.GREEN));

                    ITextComponent wrappedCoordinates = wrapInSquareBrackets(coordinatesComponent);
                    ITextComponent featureNameComponent = new TextComponentString("Dungeon");
                    ITextComponent distanceComponent = new TextComponentString(String.valueOf(distance));

                    ITextComponent finalMessage = new TextComponentTranslation(
                        "commands.locate.success",
                        featureNameComponent,
                        wrappedCoordinates,
                        distanceComponent
                    );

                    sender.sendMessage(finalMessage);
                }
            } else {
                throw new CommandException("commands.locate.fail", id);
            }
        } else {
            throw new CommandException("commands.locate.invalid_type", type);
        }
    }

    private String mapStructureIdToVanilla(String inputId) {
        String lowerCaseId = inputId.toLowerCase(Locale.ROOT);

        switch (lowerCaseId) {
            case "stronghold": return "Stronghold";
            case "mineshaft": return "Mineshaft";
            case "village":
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_taiga": return "Village";
            case "fortress":
            case "nether_fortress": return "Fortress";
            case "endcity": return "EndCity";
            case "mansion":
            case "woodland_mansion": return "Mansion";
            case "temple":
            case "desert_pyramid":
            case "jungle_pyramid":
            case "igloo":
            case "witch_hut": return "Temple";
            case "monument":
            case "ocean_monument": return "Monument";
            default:
                if (lowerCaseId.contains("_")) {
                    StringBuilder sb = new StringBuilder();
                    boolean capitalizeNext = true;
                    for (char c : lowerCaseId.toCharArray()) {
                        if (c == '_') {
                            capitalizeNext = true;
                        } else {
                            sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                            capitalizeNext = false;
                        }
                    }
                    return sb.toString();
                } else {
                    return Character.toUpperCase(lowerCaseId.charAt(0)) + lowerCaseId.substring(1);
                }
        }
    }

    private void locateBiome(MinecraftServer server, ICommandSender sender, String biomeId) throws CommandException {
        World world = sender.getEntityWorld();
        BlockPos senderPos = sender.getPosition();

        Biome targetBiome = getBiomeByName(biomeId);
        if (targetBiome == null) {
            throw new CommandException("commands.locate.biome.fail", biomeId);
        }

        int maxRadius = 5000;
        int searchStep = 32;

        BlockPos foundPos = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (int radius = searchStep; radius <= maxRadius; radius += searchStep) {
            for (int dx = -radius; dx <= radius; dx += searchStep) {
                for (int dz = -radius; dz <= radius; dz += searchStep) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    BlockPos checkPos = senderPos.add(dx, 0, dz);
                    
                    Biome biomeAtPos = world.getBiome(checkPos);
                    
                    if (biomeAtPos == targetBiome) {
                        double distanceSq = senderPos.distanceSq(checkPos);
                        if (distanceSq < minDistanceSq) {
                            minDistanceSq = distanceSq;
                            foundPos = checkPos;
                        }
                    }
                }
            }
            if (foundPos != null) {
                break;
            }
        }

        if (foundPos == null) {
            throw new CommandException("commands.locate.biome.fail", biomeId);
        } else {
            int x = foundPos.getX();
            int z = foundPos.getZ();
            
            int y = world.getHeight(x, z);

            int distance = (int) Math.sqrt(minDistanceSq);

            ITextComponent coordinatesComponent = new TextComponentString(String.format(Locale.ROOT, "%d %d %d", x, y, z));
            coordinatesComponent.setStyle(new Style()
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " " + y + " " + z))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("chat.coordinates.tooltip")))
                .setColor(TextFormatting.GREEN));

            ITextComponent wrappedCoordinates = wrapInSquareBrackets(coordinatesComponent);

            ITextComponent biomeNameComponent = new TextComponentTranslation("biome." + biomeId.replace(" ", "_"));
            ITextComponent distanceComponent = new TextComponentString(String.valueOf(distance));

            ITextComponent finalMessage = new TextComponentTranslation(
                "commands.locate.biome.success",
                biomeNameComponent,
                wrappedCoordinates,
                distanceComponent
            );

            sender.sendMessage(finalMessage);
        }
    }

    private Biome getBiomeByName(String name) {
        String searchName = name.toLowerCase(Locale.ROOT).replace("_", " ");
        for (Biome biome : Biome.REGISTRY) {
            if (biome != null && biome.getBiomeName().equalsIgnoreCase(searchName)) {
                return biome;
            }
        }
        return null;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "structure", "biome", "feature");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("structure")) {
                return getListOfStringsMatchingLastWord(args, Lists.newArrayList(
                    "stronghold", "mineshaft", "village", "fortress", "endcity", "mansion", "temple",
                    "desert_pyramid", "jungle_pyramid", "igloo", "witch_hut", "monument", "ocean_monument",
                    "woodland_mansion"
                ));
            } else if (args[0].equalsIgnoreCase("biome")) {
                List<String> biomeNames = Lists.newArrayList();
                for (Biome biome : Biome.REGISTRY) {
                    if (biome != null) {
                        biomeNames.add(biome.getBiomeName().toLowerCase(Locale.ROOT).replace(" ", "_"));
                    }
                }
                Collections.sort(biomeNames);
                return getListOfStringsMatchingLastWord(args, biomeNames);
            } else if (args[0].equalsIgnoreCase("feature")) {
                return getListOfStringsMatchingLastWord(args, "dungeon");
            }
        }
        return Collections.emptyList();
    }

    @Override
    public int compareTo(net.minecraft.command.ICommand o) {
        return 0;
    }
}