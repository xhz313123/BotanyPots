package net.darkhax.botanypots.crop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.darkhax.bookshelf.util.MCJsonUtils;
import net.darkhax.botanypots.BotanyPots;
import net.darkhax.botanypots.PacketUtils;
import net.minecraft.block.BlockState;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class CropSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CropInfo> {
    
    public static final CropSerializer INSTANCE = new CropSerializer();
    
    @Override
    public CropInfo read (ResourceLocation id, JsonObject json) {
        
        final Ingredient seed = Ingredient.deserialize(json.getAsJsonObject("seed"));
        final Set<String> validSoils = deserializeSoilInfo(id, json);
        final int growthTicks = JSONUtils.getInt(json, "growthTicks");
        final float growthModifier = JSONUtils.getFloat(json, "growthModifier");
        final List<HarvestEntry> results = deserializeCropEntries(id, json);
        final BlockState displayState = MCJsonUtils.deserializeBlockState(json.getAsJsonObject("display"));
        return new CropInfo(id, seed, validSoils, growthTicks, growthModifier, results, displayState);
    }
    
    @Override
    public CropInfo read (ResourceLocation id, PacketBuffer buf) {
        
        try {
            
            final Ingredient seed = Ingredient.read(buf);
            final Set<String> validSoils = new HashSet<>();
            PacketUtils.deserializeStringCollection(buf, validSoils);
            final int growthTicks = buf.readInt();
            final float growthModifier = buf.readFloat();
            final List<HarvestEntry> results = new ArrayList<>();
            
            // for (int i = 0; i < buf.readInt(); i++) {
            //
            // results.add(HarvestEntry.deserialize(buf));
            // }
            
            final BlockState displayState = PacketUtils.deserializeBlockState(buf);
            return new CropInfo(id, seed, validSoils, growthTicks, growthModifier, results, displayState);
        }
        
        catch (final Exception e) {
            
            e.printStackTrace();
        }
        
        throw new IllegalStateException("Failed to read crop info from packet buffer. This is not good.");
    }
    
    @Override
    public void write (PacketBuffer buffer, CropInfo info) {
        
        try {
            
            info.getSeed().write(buffer);
            PacketUtils.serializeStringCollection(buffer, info.getSoilCategories());
            buffer.writeInt(info.getGrowthTicks());
            buffer.writeFloat(info.getGrowthMultiplier());
            
            // buffer.writeInt(info.getResults().size());
            //
            // for (final HarvestEntry entry : info.getResults()) {
            //
            // HarvestEntry.serialize(buffer, entry);
            // }
            
            PacketUtils.serializeBlockState(buffer, info.getDisplayState());
        }
        
        catch (final Exception e) {
            
            e.printStackTrace();
            throw new IllegalStateException("Failed to write crop to the packet buffer.");
        }
    }
    
    /**
     * A helper method to deserialize soil categories from an array.
     * 
     * @param ownerId The Id of the SoilInfo currently being deserialized.
     * @param json The JsonObject to read from.
     * @return A set of soil categories.
     */
    private static Set<String> deserializeSoilInfo (ResourceLocation ownerId, JsonObject json) {
        
        final Set<String> categories = new HashSet<>();
        
        for (final JsonElement element : json.getAsJsonArray("categories")) {
            
            categories.add(element.getAsString().toLowerCase());
        }
        
        return categories;
    }
    
    /**
     * A helper method for reading crop harvest entries.
     * 
     * @param ownerId The id of the CropInfo being deserialized.
     * @param json The json data to read from.
     * @return A list of crop harvest entries.
     */
    private static List<HarvestEntry> deserializeCropEntries (ResourceLocation ownerId, JsonObject json) {
        
        final List<HarvestEntry> crops = new ArrayList<>();
        
        for (final JsonElement entry : json.getAsJsonArray("results")) {
            
            if (!entry.isJsonObject()) {
                
                BotanyPots.LOGGER.error("Crop entry in {} is not a JsonObject.", ownerId);
            }
            
            else {
                
                final HarvestEntry cropEntry = HarvestEntry.deserialize(entry.getAsJsonObject());
                crops.add(cropEntry);
            }
        }
        
        return crops;
    }
}
