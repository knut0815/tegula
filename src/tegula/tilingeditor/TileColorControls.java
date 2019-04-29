/*
 * TileColorControls.java Copyright (C) 2019. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tegula.tilingeditor;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import tegula.color.ColorSchemeManager;
import tegula.main.TilingStyle;
import tegula.single.SingleTilingPane;

/**
 * setup tile color controls
 * Daniel Huson, 4.2019
 */
public class TileColorControls {
    /**
     * setup controllers for color
     *
     * @param tilingEditorTab
     */
    public static void setup(TilingEditorTab tilingEditorTab) {
        final TilingEditorTabController controller = tilingEditorTab.getController();
        final SingleTilingPane singleTilingPane = tilingEditorTab.getTilingPane();
        final TilingStyle tilingStyle = singleTilingPane.getTilingStyle();

        final int numberOfTiles = singleTilingPane.getTiling().getDSymbol().countOrbits(0, 1);

        final ObservableList<Node> list = controller.getAppearanceVBox().getChildren();

        final int pos = list.indexOf(controller.getTile1ColorPicker());
        int end = pos;
        while (end < list.size() && list.get(end) instanceof ColorPicker) {
            end++;
        }

        if (end - pos > numberOfTiles) {
            list.remove(pos + numberOfTiles, end);
        } else while (end - pos < numberOfTiles) {
            final ColorPicker colorPicker = new ColorPicker();
            list.add(end++, colorPicker);
        }

        for (int t = 1; t <= numberOfTiles; t++) {
            final int tileNumber = t;
            final ColorPicker colorPicker = (ColorPicker) list.get(pos + t - 1);
            colorPicker.setOnAction((e) -> {
                tilingStyle.setTileColor(tileNumber, colorPicker.getValue());
                singleTilingPane.updateTileColors();
            });
            colorPicker.setOnShowing((e) -> {
                colorPicker.getCustomColors().setAll(ColorSchemeManager.getInstance().getColorScheme(tilingStyle.getTileColorsScheme()));
                colorPicker.setValue(tilingStyle.getTileColor(tileNumber));
            });
            colorPicker.setValue(tilingStyle.getTileColor(tileNumber));
        }
    }
}
