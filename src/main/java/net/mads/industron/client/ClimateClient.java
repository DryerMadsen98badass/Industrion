package net.mads.industron.client;

import net.mads.industron.network.ClimateStatePayload;
import net.mads.industron.climate.*;
import net.mads.industron.item.CreativeGogglesItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

public final class ClimateClient {
    private ClimateClient() {}
    private static CompoundTag data=new CompoundTag();private static long received;
    public static void accept(ClimateStatePayload payload) {
        data=payload.data().copy();received=System.currentTimeMillis();
        if(data.getBoolean("Open"))Minecraft.getInstance().setScreen(new net.mads.industron.client.screen.ClimateScreen());
    }
    public static CompoundTag data(){return data;}
    public static boolean valid(){var mc=Minecraft.getInstance();return mc.level!=null&&mc.player!=null&&System.currentTimeMillis()-received<5000
        &&mc.level.dimension().location().toString().equals(data.getString("Dimension"));}
    public static void inventory(GuiGraphics graphics,int mouseX,int mouseY,int left,int top) {
        if(!valid())return;var mc=Minecraft.getInstance();double body=data.getDouble("Body"),wet=data.getDouble("Wet");
        // A vertical strip outside the inventory, anchored to its actual GUI position.
        int x=left-28,y=top+44;
        double temperature=ClimateMath.clamp((body-33)/8,0,1);
        int color=body<36?0xff58baff:body>38?0xffff704d:0xff79d797;
        badge(graphics,x,y,color,body<=33||body>=41);
        badge(graphics,x,y+28,0xff58baff,false);
        thermometer(graphics,x+4,y+4,temperature,color);
        droplet(graphics,x+4,y+32,ClimateMath.clamp(wet,0,1));
        if(mouseX>=x&&mouseX<x+24&&mouseY>=y&&mouseY<y+24) {
            List<Component> lines=new ArrayList<>();lines.add(Component.literal("Temperature: "+ThermalRules.feeling(body)));
            if(CreativeGogglesItem.isWearing(mc.player))lines.add(Component.literal("Body: "+ClimatePresentation.number(body)+" °C"));
            lines.add(Component.literal("Clothing insulation: "+Math.round(data.getDouble("Insulation")*100)+"%"));
            lines.add(Component.literal(body<36?"Use warm, dry clothing, shelter and heat.":body>38?"Use lighter clothing, shade, water and rest.":"Your body temperature is comfortable."));
            graphics.renderComponentTooltip(mc.font,lines,mouseX,mouseY);
        }
        if(mouseX>=x&&mouseX<x+24&&mouseY>=y+28&&mouseY<y+52)
            graphics.renderComponentTooltip(mc.font,List.of(Component.literal("Wetness: "+Math.round(wet*100)+"%"),Component.literal("Wet clothes lose insulation. Dry under cover or near heat.")),mouseX,mouseY);
    }
    private static void badge(GuiGraphics g,int x,int y,int accent,boolean danger) {
        g.fill(x+1,y+1,x+25,y+25,0x88000000);
        g.fill(x,y,x+24,y+24,danger?0xffd94c43:0xff111923);
        g.fill(x+1,y+1,x+23,y+23,0xff455365);
        g.fill(x+2,y+2,x+22,y+22,0xff26313f);
        g.fill(x+2,y+2,x+22,y+3,0xff697c91);
        g.fill(x+2,y+21,x+22,y+22,0xff18222f);
        g.fill(x+3,y+19,x+5,y+21,accent);
    }
    private static void thermometer(GuiGraphics g,int x,int y,double amount,int color) {
        String[] outline={"......####......",".....######.....",".....######.....",".....######.....",
            ".....######.....",".....######.....",".....######.....",".....######.....",
            ".....######.....","....########....","...##########...","...##########...",
            "...##########...","....########....",".....######.....","................"};
        pixels(g,x,y,outline,0xff101722);
        g.fill(x+6,y+1,x+10,y+10,0xffd6e4e9);
        g.fill(x+7,y+2,x+9,y+10,0xff425564);
        int height=1+(int)Math.round(amount*7);
        g.fill(x+7,y+10-height,x+9,y+11,color);
        g.fill(x+5,y+10,x+11,y+13,color);
        g.fill(x+6,y+13,x+10,y+14,color);
        g.fill(x+5,y+10,x+6,y+12,0xffe1f4fc);
        for(int row=2;row<=8;row+=3)g.fill(x+11,y+row,x+14,y+row+1,0xffb7c7d3);
    }
    private static void droplet(GuiGraphics g,int x,int y,double wet) {
        String[] shape={".......##.......","......####......","......####......",".....######.....",
            ".....######.....","....########....","...##########...","...##########...",
            "..############..","..############..","..############..","..############..",
            "...##########...","....########....",".....######.....","................"};
        int surface=14-(int)Math.round(wet*13);
        for(int row=0;row<shape.length;row++)for(int col=0;col<16;col++)if(shape[row].charAt(col)=='#') {
            boolean edge=col==0||col==15||shape[row].charAt(col-1)!='#'||shape[row].charAt(col+1)!='#'
                ||row==0||row==14||shape[row-1].charAt(col)!='#'||shape[row+1].charAt(col)!='#';
            int color=edge?0xff0f1928:row>=surface?(col<8?0xff62baf3:0xff3184ca):0xff3b526a;
            g.fill(x+col,y+row,x+col+1,y+row+1,color);
        }
        g.fill(x+5,y+7,x+6,y+10,0xffc0e9fc);
        g.fill(x+6,y+6,x+7,y+7,0xffe4f7ff);
    }
    private static void pixels(GuiGraphics g,int x,int y,String[] rows,int color) {
        for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++)
            if(rows[row].charAt(col)=='#')g.fill(x+col,y+row,x+col+1,y+row+1,color);
    }
    public static void renderOverlay(GuiGraphics graphics,DeltaTracker delta) {
        var mc=Minecraft.getInstance();if(!valid()||mc.options.hideGui||mc.screen!=null)return;
        double body=data.getDouble("Body");
        if(body<35||body>39)graphics.drawString(mc.font,ThermalRules.feeling(body),8,8,body<35?0x77bbff:0xff7755);
        if(!CreativeGogglesItem.isWearing(mc.player)||!(mc.hitResult instanceof BlockHitResult hit)||!data.contains("Plant")||hit.getBlockPos().asLong()!=data.getLong("Target"))return;
        CompoundTag p=data.getCompound("Plant");List<Component> lines=new ArrayList<>();
        lines.add(Component.literal("Growth: "+Math.round(p.getDouble("Growth")/Math.max(1,p.getDouble("Required"))*100)+"%"));
        lines.add(Component.literal("Suitable seasons: "+p.getString("Seasons")));
        lines.add(Component.literal("Estimated harvest: "+(p.getDouble("Harvest")<0?"Not within the next two years":"Day "+((long)Math.floor(p.getDouble("Harvest"))+1))));
        if(!p.getString("Stopped").isEmpty())lines.add(Component.literal("Growth stopped: "+p.getString("Stopped")));
        if(Screen.hasShiftDown()) {
            lines.add(Component.literal("Planted: Day "+((long)Math.floor(p.getDouble("Planted"))+1)));
            lines.add(Component.literal("Effective growth: "+ClimatePresentation.number(p.getDouble("Growth"))+" / "+ClimatePresentation.number(p.getDouble("Required"))+" days"));
            lines.add(Component.literal("Temperature: "+ClimatePresentation.number(p.getDouble("ActualTemperature"))+" °C; range "+ClimatePresentation.number(p.getDouble("Minimum"))+"–"+ClimatePresentation.number(p.getDouble("Maximum"))+" °C"));
            lines.add(Component.literal("Humidity: "+ClimatePresentation.number(p.getDouble("Humidity")*100)+"%; minimum "+ClimatePresentation.number(p.getDouble("MinimumHumidity")*100)+"%"));
            lines.add(Component.literal("Growth multiplier: "+ClimatePresentation.number(p.getDouble("Rate"))+"×"));
            lines.add(Component.literal("Last update: Day "+ClimatePresentation.number(p.getDouble("Last")+1)));
            lines.add(Component.literal("Last catch-up: "+ClimatePresentation.number(p.getDouble("LastAdded"))+" effective days over "+ClimatePresentation.number(p.getDouble("LastElapsed"))+" days"));
            if(p.contains("UnloadedAdded"))lines.add(Component.literal("Unloaded growth: "+ClimatePresentation.number(p.getDouble("UnloadedAdded"))+" effective days over "+ClimatePresentation.number(p.getDouble("UnloadedDays"))+" days"));
            lines.add(Component.literal("Climate stress: "+Math.round(p.getDouble("Stress")*100)+"%"));
        }
        int x=Math.min(graphics.guiWidth()-230,graphics.guiWidth()/2+12),y=Math.max(10,graphics.guiHeight()/2-25);
        graphics.renderComponentTooltip(mc.font,lines,Math.max(8,x),y);
    }
}
