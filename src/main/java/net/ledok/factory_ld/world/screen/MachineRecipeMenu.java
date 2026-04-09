package net.ledok.factory_ld.world.screen;

import java.util.List;
import java.util.Optional;

import net.ledok.factory_ld.world.recipe.view.MachineRecipeView;

public interface MachineRecipeMenu {
    List<MachineRecipeView> getAvailableRecipeViews();

    Optional<MachineRecipeView> getRecipeView(int recipeIndex);

    Optional<MachineRecipeView> getSelectedRecipeView();

    void setMachineSlotsActive(boolean active);

    void setPlayerSlotsActive(boolean active);
}
