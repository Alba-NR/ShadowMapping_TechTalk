package graphics.renderEngine.renderOptionsManager;

import java.util.Arrays;
import java.util.List;


public class RenderOptionsManager {
    private static List<RenderOptions> mapIntIDtoOptions = Arrays.asList(
      RenderOptions.NORMAL,
      RenderOptions.FROM_LIGHT_POV,
      RenderOptions.DEPTH_MAP,
      RenderOptions.WITH_SHADOWS
    );
    private static int numOfOptions = mapIntIDtoOptions.size();

    /**
     * Returns the option associated w/the given int id.
     * @param id int id number of a render option
     * @return {@link RenderOptions} to which that id belongs
     */
    public static RenderOptions getEffectByIntID(int id){
        if(id >= 1 && id <= mapIntIDtoOptions.size()) return mapIntIDtoOptions.get(id-1);
        else return RenderOptions.NORMAL;
    }

    /**
     * Returns the nº of render options
     */
    public static int getNumOfOptions(){
        return numOfOptions;
    }
}
