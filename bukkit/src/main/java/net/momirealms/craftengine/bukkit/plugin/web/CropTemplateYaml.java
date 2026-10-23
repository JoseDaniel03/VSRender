package net.momirealms.craftengine.bukkit.plugin.web;

/**
 * Only YAML templates using VSRender's existing crop_block / bush_block /
 * block_item system. No additional gameplay engine or listener.
 */
final class CropTemplateYaml {
    private CropTemplateYaml() {}

    static String generate(String itemId,String name,String productYaml,
                           int stages,double speed,String seedTexture) {
        if(stages<2||stages>8||speed<=0||speed>1)
            throw new IllegalArgumentException("Etapas o velocidad invalidas");
        String seeds=itemId+"_semillas",crop=itemId+"_cultivo";
        String safeName=name.replace("'","''").replace("\n"," ").replace("\r"," ");
        StringBuilder s=new StringBuilder(productYaml);
        if(!productYaml.endsWith("\n"))s.append('\n');
        s.append("  ").append(seeds).append(":\n")
         .append("    material: wheat_seeds\n")
         .append("    data:\n")
         .append("      item_name: '<!i><green>Semillas de ").append(safeName).append("'\n")
         .append("    behavior:\n")
         .append("      type: block_item\n")
         .append("      block: ").append(crop).append('\n');
        if(!seedTexture.isBlank())s.append("    texture: ").append(seedTexture).append('\n');
        s.append("blocks:\n  ").append(crop).append(":\n")
         .append("    settings:\n")
         .append("      template:\n")
         .append("        - default:hardness/none\n")
         .append("        - default:sound/grass\n")
         .append("      overrides:\n")
         .append("        item: ").append(seeds).append("\n")
         .append("        tags:\n")
         .append("          - minecraft:crops\n")
         .append("          - minecraft:maintains_farmland\n")
         .append("    behaviors:\n")
         .append("      - type: liquid_flowable_block\n")
         .append("      - type: bush_block\n")
         .append("        bottom_blocks:\n")
         .append("          - minecraft:farmland\n")
         .append("      - type: crop_block\n")
         .append("        grow_speed: ").append(speed).append("\n")
         .append("        light_requirement: 9\n")
         .append("        is_bone_meal_target: true\n")
         .append("        bone_meal_age_bonus: 1\n")
         .append("    loot:\n")
         .append("      template: default:loot_table/seed_crop\n")
         .append("      arguments:\n")
         .append("        crop_item: ").append(itemId).append("\n")
         .append("        crop_seed: ").append(seeds).append("\n")
         .append("        ripe_age: ").append(stages-1).append("\n")
         .append("    states:\n")
         .append("      properties:\n")
         .append("        age:\n")
         .append("          type: int\n")
         .append("          default: 0\n")
         .append("          min: 0\n")
         .append("          max: ").append(stages-1).append("\n")
         .append("      appearances:\n");
        for(int age=0;age<stages;age++){
            int wheatAge=Math.round(age*7f/(stages-1));
            s.append("        stage_").append(age).append(":\n")
             .append("          state: wheat[age=").append(wheatAge).append("]\n");
        }
        s.append("      variants:\n");
        for(int age=0;age<stages;age++){
            s.append("        age=").append(age).append(":\n")
             .append("          appearance: stage_").append(age).append('\n');
        }
        return s.toString();
    }
}
