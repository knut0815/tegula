/*
 * ControlBindings.java Copyright (C) 2019. Daniel H. Huson
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.paint.Color;
import jloda.fx.undo.UndoManager;
import jloda.fx.undo.UndoableChangeProperty;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.ColorSchemeManager;
import tegula.core.dsymbols.DSymbolAlgorithms;
import tegula.core.dsymbols.Geometry;
import tegula.main.TilingStyle;
import tegula.single.SingleTilingPane;
import tegula.undoable.ChangeDSymbolCommand;
import tegula.util.HasHyperbolicModel;

/**
 * setup control bindings
 * Daniel Huson, 4.2019
 */
public class ControlBindings {
    public static void setup(TilingEditorTab tilingEditorTab) {
        final SingleTilingPane tilingPane = tilingEditorTab.getTilingPane();
        final TilingStyle tilingStyle = tilingEditorTab.getTilingStyle();
        final TilingEditorTabController controller = tilingEditorTab.getController();
        final UndoManager undoManager = tilingEditorTab.getUndoManager();

        controller.getModelChoiceBox().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo()) {
                final HasHyperbolicModel.HyperbolicModel oldModel = tilingPane.getHyperbolicModel();
                final HasHyperbolicModel.HyperbolicModel newModel = HasHyperbolicModel.HyperbolicModel.valueOf(controller.getModelChoiceBox().getSelectionModel().getSelectedItem());
                undoManager.doAndAdd(new UndoableChangeProperty<>("hyperbolic model", tilingPane.hyperbolicModelProperty(), oldModel, newModel,
                        (v) -> {
                            controller.getModelChoiceBox().getSelectionModel().select(v.toString());
                        }));
            }
        });
        controller.getModelChoiceBox().disableProperty().bind(tilingPane.geometryProperty().isNotEqualTo(Geometry.Hyperbolic));

        controller.getShowMoreTilesButton().setOnAction((e) -> undoManager.doAndAdd(new UndoableRedoableCommand("show more tiles") {
            @Override
            public void undo() {
                tilingPane.decreaseTiling();
            }

            @Override
            public void redo() {
                tilingPane.increaseTiling();
            }
        }));
        controller.getShowMoreTilesButton().disableProperty().bind(tilingPane.geometryProperty().isNotEqualTo(Geometry.Hyperbolic));

        controller.getShowLessTilesButton().setOnAction((e) -> undoManager.doAndAdd(new UndoableRedoableCommand("show less tiles") {
            @Override
            public void undo() {
                tilingPane.increaseTiling();
            }

            @Override
            public void redo() {
                tilingPane.decreaseTiling();
            }
        }));
        controller.getShowLessTilesButton().disableProperty().bind(tilingPane.geometryProperty().isNotEqualTo(Geometry.Hyperbolic).or(
                tilingPane.getTiling().canDecreaseTilingProperty().not()));

        controller.getGroupTextField().textProperty().bind(tilingPane.groupNameProperty());

        controller.getStraightenEdgesButton().setOnAction((e) -> {


            final Point2D[][] oldCoordinates = tilingPane.getTiling().getfDomain().getCoordinates();
            undoManager.doAndAdd(new UndoableRedoableCommand("straighten") {
                public void undo() {
                    tilingPane.changeCoordinates(oldCoordinates);
                    tilingPane.update();
                }

                public void redo() {
                    StraightenEdges.straightenAllEdges(tilingPane.getTiling().getfDomain());
                    tilingPane.update();
                }
            });
        });

        controller.getDualizeButton().setOnAction((e) -> {
            final Point2D[][] coordinates = tilingPane.getTiling().getfDomain().getCoordinates();
            undoManager.doAndAdd(new ChangeDSymbolCommand("dualize", tilingPane.getTiling().getDSymbol(), DSymbolAlgorithms.dualize(tilingPane.getTiling().getDSymbol()),
                    tilingPane::replaceTiling, coordinates, tilingPane::changeCoordinates));
        });

        controller.getMaximizeButton().setOnAction((e) -> {
            final Point2D[][] coordinates = tilingPane.getTiling().getfDomain().getCoordinates();
            undoManager.doAndAdd(new ChangeDSymbolCommand("maximize", tilingPane.getTiling().getDSymbol(), DSymbolAlgorithms.maxSymmetry(tilingPane.getTiling().getDSymbol()),
                    tilingPane::replaceTiling, coordinates, tilingPane::changeCoordinates));

        });
        controller.getMaximizeButton().disableProperty().bind(tilingPane.maximalTilingProperty());

        controller.getOrientateButton().setOnAction((e) -> {
            final Point2D[][] coordinates = tilingPane.getTiling().getfDomain().getCoordinates();
            undoManager.doAndAdd(new ChangeDSymbolCommand("orientate", tilingPane.getTiling().getDSymbol(), DSymbolAlgorithms.orientate(tilingPane.getTiling().getDSymbol()),
                    tilingPane::replaceTiling, coordinates, tilingPane::changeCoordinates));

        });
        controller.getOrientateButton().disableProperty().bind(tilingPane.orientableTilingProperty());

        controller.getBandWidthSpinner().setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, tilingStyle.getBandWidth()));

        controller.getBandWidthSpinner().valueProperty().addListener((c, o, n) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("width",
                        tilingStyle.bandWidthProperty(), tilingStyle.getBandWidth(), n,
                        (v) -> {
                            tilingPane.update();
                            controller.getBandWidthSpinner().getValueFactory().setValue(v.intValue());
                        }));
            tilingStyle.setBandWidth(n);
            tilingPane.update();
        });

        controller.getShowFacesCheckBox().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("tiles",
                        tilingStyle.showFacesProperty(), !controller.getShowFacesCheckBox().isSelected(),
                        controller.getShowFacesCheckBox().isSelected(),
                        (v) -> {
                            tilingPane.update();
                            controller.getShowFacesCheckBox().setSelected(v);
                        }));
        });

        controller.getBackFacesCheckBox().setSelected(tilingStyle.isShowBackFaces());
        controller.getBackFacesCheckBox().setOnAction((e) -> {
            tilingStyle.setShowBackFaces(controller.getBackFacesCheckBox().isSelected());
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("back faces",
                        tilingStyle.showBackFacesProperty(), !controller.getBackFacesCheckBox().isSelected(),
                        controller.getBackFacesCheckBox().isSelected(),
                        (v) -> {
                            tilingPane.updateTileColors();
                            controller.getBackBandsCheckBox().setSelected(v);
                        }));
        });
        tilingStyle.showBackFacesProperty().addListener((c, o, n) -> controller.getBackFacesCheckBox().setSelected(n));
        controller.getBackFacesCheckBox().disableProperty().bind(tilingPane.geometryProperty().isEqualTo(Geometry.Euclidean));

        final DoubleProperty tileOpacity = new SimpleDoubleProperty(1);
        controller.getTilesOpacitySlider().valueProperty().addListener((c, o, n) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("tile opacity",
                        tileOpacity, tileOpacity.get(), n,
                        (v) -> {
                            final int numTiles = tilingPane.getTiling().getDSymbol().countOrbits(0, 1);
                            for (int t = 1; t <= numTiles; t++) {
                                final Color color = tilingStyle.getTileColor(t);
                                tilingStyle.setTileColor(t, new Color(color.getRed(), color.getGreen(), color.getBlue(), tileOpacity.getValue()));
                            }
                            tilingPane.updateTileColors();
                            controller.getTilesOpacitySlider().setValue(v.doubleValue());
                        })
                );
        });
        controller.getTilesOpacitySlider().setValue(0.8);
        undoManager.clear(); // don't want to keep this event


        controller.getShowBandsCheckBox().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("bands",
                        tilingStyle.showBandsProperty(), !controller.getShowBandsCheckBox().isSelected(),
                        controller.getShowBandsCheckBox().isSelected(),
                        (v) -> {
                            tilingPane.update();
                            controller.getShowBandsCheckBox().setSelected(v);
                        }));
        });

        controller.getBackBandsCheckBox().setSelected(tilingStyle.isShowBackBands());
        controller.getBackBandsCheckBox().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("back bands",
                        tilingStyle.showBackBandsProperty(), !controller.getBackBandsCheckBox().isSelected(),
                        controller.getBackBandsCheckBox().isSelected(),
                        (v) -> {
                            controller.getBackBandsCheckBox().setSelected(v);
                            tilingPane.update();
                        }));
        });
        controller.getBackBandsCheckBox().disableProperty().bind(tilingPane.geometryProperty().isEqualTo(Geometry.Euclidean));


        controller.getSmoothEdgesCheckBox().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("smooth",
                        tilingStyle.smoothEdgesProperty(), !controller.getSmoothEdgesCheckBox().isSelected(),
                        controller.getSmoothEdgesCheckBox().isSelected(),
                        (v) -> {
                            tilingPane.update();
                            controller.getSmoothEdgesCheckBox().setSelected(v);
                        }
                ));
        });
        controller.getSmoothEdgesCheckBox().disableProperty().bind(tilingPane.geometryProperty().isNotEqualTo(Geometry.Spherical));


        controller.getBandsColorPicker().setValue(tilingStyle.getBandColor());
        controller.getBandsColorPicker().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("band color",
                        tilingStyle.bandColorProperty(), tilingStyle.getBandColor(), controller.getBandsColorPicker().getValue(),
                        (v) -> tilingPane.updateBandColors()));
        });
        tilingStyle.bandColorProperty().addListener((c, o, n) -> {
            controller.getBandsColorPicker().setValue(n);
            controller.getBandsOpacitySlider().setValue(n.getOpacity());
        });

        controller.getBandsColorPicker().setOnShowing((e) -> {
            controller.getBandsColorPicker().getCustomColors().setAll(ColorSchemeManager.getInstance().getColorScheme(tilingStyle.getTileColorsScheme()));
            controller.getBandsColorPicker().setValue(tilingStyle.getBandColor());
        });

        controller.getBandsOpacitySlider().setValue(tilingStyle.getBandColor().getOpacity());
        controller.getBandsOpacitySlider().valueProperty().addListener((c, o, n) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("band opacity",
                        tilingStyle.bandColorProperty(), tilingStyle.getBandColor(),
                        new Color(tilingStyle.getBandColor().getRed(), tilingStyle.getBandColor().getGreen(), tilingStyle.getBandColor().getBlue(), n.doubleValue()),
                        (v) -> {
                            tilingPane.updateBandColors();
                            controller.getBandsOpacitySlider().setValue(v.getOpacity());
                        }));
        });

        controller.getBackgroundColorPicker().setOnAction((e) -> {
            if (!undoManager.isPerformingUndoOrRedo())
                undoManager.doAndAdd(new UndoableChangeProperty<>("background",
                        tilingStyle.backgroundColorProperty(), tilingStyle.getBackgroundColor(), controller.getBackgroundColorPicker().getValue(),
                        null));
        });
        tilingStyle.backgroundColorProperty().addListener((c, o, n) -> controller.getBackgroundColorPicker().setValue(n));
        controller.getBackgroundColorPicker().disableProperty().bind(tilingPane.geometryProperty().isEqualTo(Geometry.Euclidean));

        controller.getBackgroundColorPicker().setOnShowing((e) -> {
            controller.getBackgroundColorPicker().setValue(tilingStyle.getBackgroundColor());
        });

        controller.getInfoTextField().textProperty().bind(tilingPane.infoLineProperty());

        {
            controller.getColorSchemeChoiceBox().getSelectionModel().select(tilingStyle.getTileColorsScheme());
            for (String colorSchemeName : ColorSchemeManager.getInstance().getNames()) {
                controller.getColorSchemeChoiceBox().getItems().add(colorSchemeName);
            }
            controller.getColorSchemeChoiceBox().getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
                if (!undoManager.isPerformingUndoOrRedo()) {
                    undoManager.doAndAdd(new UndoableChangeProperty<>("colors",
                            tilingStyle.tileColorsSchemeProperty(), tilingStyle.getTileColorsScheme(), n,
                            (v) -> {
                                tilingPane.updateTileColors();
                                controller.getColorSchemeChoiceBox().setValue(v);
                            }));
                }
            });
        }


        controller.getStopAnimationButton().setVisible(false);
        if (tilingPane.getMouseHandler() != null) {
            controller.getStopAnimationButton().setOnAction((e) -> tilingPane.getMouseHandler().getAnimator().stop());
            controller.getStopAnimationButton().visibleProperty().bind(tilingPane.getMouseHandler().getAnimator().playingProperty());
        }

        controller.getHyperbolicModelAccordion().disableProperty().bind(tilingPane.geometryProperty().isNotEqualTo(Geometry.Hyperbolic));
        tilingPane.geometryProperty().addListener((c, o, n) -> {
            if (n != Geometry.Hyperbolic)
                controller.getHyperbolicModelAccordion().setExpandedPane(null);
        });
    }
}
