package net.mads.industron.client.screen;
import net.mads.industron.client.ClimateClient;
import net.mads.industron.climate.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
public final class ClimateScreen extends Screen {
    public ClimateScreen(){super(Component.literal("Climate instrument"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        super.render(g,mouseX,mouseY,partial);int x=Math.max(8,width/2-140),y=Math.max(8,height/2-95);
        g.fill(x-8,y-8,x+272,y+180,0xee17222a);g.drawString(font,title,x,y,0xffcc88);
        if(!ClimateClient.valid()){g.drawString(font,"Waiting for climate data…",x,y+24,0xffffff);return;}
        var d=ClimateClient.data();double day=d.getDouble("Day");boolean seasonal=d.getBoolean("Seasonal");
        List<String> lines=new ArrayList<>();
        lines.add(seasonal?"Season: "+ClimateMath.seasonName(day)+" — day "+(Math.floorMod((long)Math.floor(day),25)+1)+"/25":"No seasonal cycle in this dimension");
        lines.add("World day: "+((long)Math.floor(day)+1));
        if(seasonal)lines.add("Daylight: "+ClimatePresentation.number(ClimateMath.daylightTicks(day)/1000)+" Minecraft hours");
        lines.add("Local temperature: "+ClimatePresentation.number(d.getDouble("Ambient"))+" °C");
        if(seasonal)lines.add("Seasonal adjustment: "+ClimatePresentation.number(ClimateMath.seasonalOffset(day))+" °C");
        lines.add("Local humidity: "+Math.round(d.getDouble("Humidity")*100)+"%");
        lines.add("Precipitation: "+(d.getBoolean("Snow")?"Snow":d.getBoolean("Rain")?"Rain":"None at your position"));
        lines.add("Wind exposure: "+Math.round(d.getDouble("Wind")*100)+"%");
        lines.add("Shelter: "+(d.getBoolean("Roof")?"Roof, "+d.getInt("Walls")+" sheltered sides":"Exposed"));
        int row=y+22;for(String line:lines){g.drawString(font,line,x,row,0xe1edf4);row+=15;}
    }
}
