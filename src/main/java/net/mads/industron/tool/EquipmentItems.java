package net.mads.industron.tool;

import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Locale;

/** Native item classes keep vanilla ammunition, blocking, shearing, fishing and equipping behaviour. */
public final class EquipmentItems {
    private EquipmentItems() {}
    public static Item create(ToolDefinition def, Item.Properties properties) {
        if (def.id().equals("sword")) return new MaterialSword(def,properties);
        if (!EquipmentStats.isEquipment(def.id()) && !def.id().equals("shears")) return new ComposedToolItem(def,properties);
        Item.Properties p=ComposedToolItem.withDefaultToolData(def,properties);
        return switch(def.id()) {
            case "shears" ->new MaterialShears(def,p); case "bow" ->new MaterialBow(def,p);
            case "crossbow" ->new MaterialCrossbow(def,p);case "shield" ->new MaterialShield(def,p);
            case "fishing_rod" ->new MaterialFishingRod(def,p);
            case "helmet" ->new MaterialArmour(def,ArmorItem.Type.HELMET,p);
            case "chestplate" ->new MaterialArmour(def,ArmorItem.Type.CHESTPLATE,p);
            case "leggings" ->new MaterialArmour(def,ArmorItem.Type.LEGGINGS,p);
            case "boots" ->new MaterialArmour(def,ArmorItem.Type.BOOTS,p);
            default ->throw new IllegalArgumentException(def.id());
        };
    }
    public static ItemAttributeModifiers armourAttributes(ToolDefinition def,ToolStackData data){
            EquipmentStats s=EquipmentStats.calculate(def,data);if(s==null)return ItemAttributeModifiers.EMPTY;
            EquipmentSlotGroup group=switch(def.id()){
                case "helmet" ->EquipmentSlotGroup.HEAD;case "chestplate" ->EquipmentSlotGroup.CHEST;
                case "leggings" ->EquipmentSlotGroup.LEGS;default ->EquipmentSlotGroup.FEET;
            };
            var id=ResourceLocation.fromNamespaceAndPath("industron","armour_"+def.id());
            return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,new AttributeModifier(id,s.armour(),AttributeModifier.Operation.ADD_VALUE),group)
                .add(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(id,s.toughness(),AttributeModifier.Operation.ADD_VALUE),group)
                .add(Attributes.MOVEMENT_SPEED,new AttributeModifier(id,-Math.min(.06,s.weight()*.002),AttributeModifier.Operation.ADD_MULTIPLIED_BASE),group).build();
        }
    private static String number(double v){return String.format(Locale.ROOT,"%.2f",v);}
    private static void tooltip(MaterialEquipment item,ItemStack stack,List<Component> out){
        ToolStats tool=item.stats(stack);if(tool==null)return;
        EquipmentStats s=EquipmentStats.of(stack);
        if(s==null){
            out.add(Component.literal("Durability: "+Math.max(0,stack.getMaxDamage()-stack.getDamageValue())+" / "+stack.getMaxDamage()));
            if(ToolStatPresentation.showsEfficiency(item.definition()))out.add(Component.literal("Efficiency: "+number(tool.efficiencySeconds())+" s"));
            if(ToolStatPresentation.showsDamage(item.definition()))out.add(Component.literal("Damage: "+number(tool.damage())));
            return;
        }
        EquipmentTooltip.appendFinished(item.definition().id(),stack,s,out);
    }
    public static final class MaterialShears extends ShearsItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialShears(ToolDefinition d,Properties p){super(p);def=d;}
        public ToolDefinition definition(){return def;}
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
    public static final class MaterialBow extends BowItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialBow(ToolDefinition d,Properties p){super(p);def=d;}
        public ToolDefinition definition(){return def;}
        @Override protected void shoot(net.minecraft.server.level.ServerLevel level, LivingEntity entity,
                InteractionHand hand, ItemStack stack, List<ItemStack> projectiles,
                float speed, float spread, boolean critical, LivingEntity target) {
            EquipmentStats stats=EquipmentStats.of(stack);
            super.shoot(level,entity,hand,stack,projectiles,
                    stats==null?speed:speed*(float)Math.sqrt(stats.energy()),spread,critical,target);
        }
        @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
            EquipmentStats s=EquipmentStats.of(stack);if(s==null)return;
            int used=getUseDuration(stack,entity)-remaining;
            int virtualUsed=(int)Math.floor(used*20.0/s.drawTicks());
            super.releaseUsing(stack,level,entity,getUseDuration(stack,entity)-virtualUsed);
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
    public static final class MaterialCrossbow extends CrossbowItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialCrossbow(ToolDefinition d,Properties p){super(p);def=d;}
        public ToolDefinition definition(){return def;}
        private int chargeTicks(ItemStack stack,LivingEntity entity){
            int vanilla=CrossbowItem.getChargeDuration(stack,entity);
            EquipmentStats s=EquipmentStats.of(stack);
            return s==null || !(entity instanceof Player)?vanilla:
                    Math.max(1,(int)Math.round(s.drawTicks()*vanilla/25.0));
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){
            return chargeTicks(stack,entity)+3;
        }
        private int virtualRemaining(ItemStack stack,LivingEntity entity,int remaining){
            EquipmentStats s=EquipmentStats.of(stack);if(s==null || !(entity instanceof Player))return remaining;
            // Vanilla's enchantment-adjusted charge duration is retained (including Quick Charge).
            int vanilla=CrossbowItem.getChargeDuration(stack,entity);
            int target=chargeTicks(stack,entity);
            int used=Math.max(0,getUseDuration(stack,entity)-remaining);
            int virtualUsed=(int)Math.floor(Math.min(used,target)*(double)vanilla/target);
            return getUseDuration(stack,entity)-virtualUsed;
        }
        @Override public void onUseTick(Level l,LivingEntity e,ItemStack s,int remaining){
            int virtual=virtualRemaining(s,e,remaining);
            super.onUseTick(l,e,s,virtual);
            if(e instanceof Player && getUseDuration(s,e)-remaining>=chargeTicks(s,e)){
                // Load once at the material's actual charge threshold. Keep vanilla's
                // ammunition consumption, Multishot and charged-projectile component.
                if(!CrossbowItem.isCharged(s))super.releaseUsing(s,l,e,virtual);
                if(CrossbowItem.isCharged(s))e.stopUsingItem();
            }
        }
        @Override public void releaseUsing(ItemStack s,Level l,LivingEntity e,int remaining){super.releaseUsing(s,l,e,virtualRemaining(s,e,remaining));}
        @Override public void performShooting(Level l,LivingEntity e,InteractionHand hand,ItemStack stack,float speed,float spread,LivingEntity target){
            EquipmentStats s=EquipmentStats.of(stack);
            super.performShooting(l,e,hand,stack,s==null?speed:speed*(float)Math.sqrt(s.energy()/1.5),spread,target);
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
    public static final class MaterialShield extends ShieldItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialShield(ToolDefinition d,Properties p){super(p);def=d;}
        public ToolDefinition definition(){return def;}
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
    public static final class MaterialFishingRod extends FishingRodItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialFishingRod(ToolDefinition d,Properties p){super(p);def=d;}
        public ToolDefinition definition(){return def;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            boolean casting=player.fishing==null;
            ItemStack stack=player.getItemInHand(hand); EquipmentStats s=EquipmentStats.of(stack);
            int oldDamage=stack.getDamageValue();
            var result=super.use(level,player,hand);
            if (!casting && !level.isClientSide && s!=null && !stack.isEmpty()) {
                int nativeWear=Math.max(0,stack.getDamageValue()-oldDamage);
                int extraWear=Math.max(0,(int)Math.ceil(nativeWear*10/s.lineLoad())-nativeWear);
                if(extraWear>0)stack.hurtAndBreak(extraWear,player,LivingEntity.getSlotForHand(hand));
            }
            if(casting && player.fishing!=null && s!=null){player.fishing.setDeltaMovement(player.fishing.getDeltaMovement().scale(Math.sqrt(s.castDistance()/24.0)));}
            return result;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
    public static final class MaterialArmour extends ArmorItem implements MaterialEquipment {
        private final ToolDefinition def;
        MaterialArmour(ToolDefinition d,Type type,Properties p){super(ArmorMaterials.IRON,type,p);def=d;}
        public ToolDefinition definition(){return def;}
        @Override public boolean makesPiglinsNeutral(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer){
            ToolStackData parts = data(stack);
            return parts != null && ("industrial/" + net.mads.industron.progression.ProgressionMaterials.GOLD)
                    .equals(parts.materialKey("shell"));
        }
        @Override public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack){
            return armourAttributes(def,data(stack));
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> t,TooltipFlag f){super.appendHoverText(s,c,t,f);tooltip(this,s,t);}
    }
}
