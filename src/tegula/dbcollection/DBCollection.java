/*
 * DBCollection.java Copyright (C) 2019. Daniel H. Huson
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

package tegula.dbcollection;

import javafx.beans.property.*;
import jloda.fx.util.AService;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import tegula.core.dsymbols.DSymbol;
import tegula.db.DatabaseAccess;
import tegula.util.IFileBased;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * opens a collection of tilings from a database
 * Daniel Huson, 10.2019
 */
public class DBCollection implements Closeable, IFileBased {
    private final DatabaseAccess databaseAccess;
    private final StringProperty fileName = new SimpleStringProperty();

    private final StringProperty dbSelect = new SimpleStringProperty("");
    private final IntegerProperty count = new SimpleIntegerProperty(0);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(20);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);

    private AService<ArrayList<DSymbol>> service = null;


    /**
     * constructor
     *
     * @param databaseFile
     * @throws IOException
     * @throws SQLException
     */
    public DBCollection(String databaseFile) throws IOException, SQLException {
        fileName.set(databaseFile);
        this.databaseAccess = new DatabaseAccess(databaseFile);
        totalCount.set(databaseAccess.computeDBSize());

        dbSelect.addListener((c, o, n) -> {
            try {
                count.set(databaseAccess.countDSymbols(getDbSelect()));
            } catch (IOException | SQLException e) {
                Basic.caught(e);
                count.set(0);
            }
        });
    }

    public int getNumberOfPages() {
        return (int) Math.ceil((float) getCount() / getPageSize());
    }

    public int getNumberOfDSymbolsOnPage(int pageNumber) {
        return pageNumber < getNumberOfPages() ? getPageSize() : getCount() - pageNumber * getPageSize();
    }

    /**
     * load page of DSymbols
     *
     * @param pageNumber
     * @param consumeResult
     */
    public void loadPageOfDSymbols(int pageNumber, Consumer<ArrayList<DSymbol>> consumeResult) {
        synchronized (this) {
            if (service != null && service.isRunning())
                service.cancel();

            final AService<ArrayList<DSymbol>> service = new AService<>();
            this.service = service;
            service.setOnCancelled((e) -> consumeResult.accept(service.getValue()));
            service.setOnSucceeded((e) -> consumeResult.accept(service.getValue()));
            service.setOnFailed((e) -> Basic.caught(service.getException()));
            service.setCallable(() -> getPageOfDSymbols(pageNumber));
            service.start();
        }
    }

    /**
     * get all D-symbols for the given page number
     *
     * @param pageNumber 1-based
     * @return D-symbols
     * @throws IOException
     * @throws SQLException
     */
    public ArrayList<DSymbol> getPageOfDSymbols(int pageNumber) throws IOException, SQLException {
        if (pageNumber < 0 || pageNumber >= getNumberOfPages())
            return new ArrayList<>();
        final String query;
        if (getDbSelect().length() == 0)
            query = String.format("select symbol from tilings where cardinality>0 limit %d offset %d;", getPageSize(), pageNumber * getPageSize());
        else
            query = String.format("select symbol from tilings where %s limit %d offset %d;", getDbSelect(), getPageSize(), pageNumber * getPageSize());
        final ArrayList<DSymbol> result = new ArrayList<>();
        for (String line : databaseAccess.getDSymbols(query))
            result.add(new DSymbol(line));
        return result;
    }

    public String getDbSelect() {
        return dbSelect.get();
    }

    public StringProperty dbSelectProperty() {
        return dbSelect;
    }

    public void setDbSelect(String dbSelect) {
        this.dbSelect.set(dbSelect);
    }

    public int getCount() {
        return count.get();
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count;
    }

    public int getPageSize() {
        return pageSize.get();
    }

    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize.set(pageSize);
    }

    @Override
    public void close() throws IOException {
        databaseAccess.close();
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public ReadOnlyIntegerProperty totalCountProperty() {
        return totalCount;
    }

    @Override
    public String getFileName() {
        return fileName.get();
    }

    @Override
    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        //this.fileName.set(fileName);
    }

    @Override
    public String getTitle() {
        if (getDbSelect().length() == 0)
            return String.format("%s - %s", Basic.getFileNameWithoutPath(getFileName()), ProgramProperties.getProgramVersion());
        else
            return String.format("%s (%s) - %s", Basic.getFileNameWithoutPath(getFileName()), getDbSelect(), ProgramProperties.getProgramVersion());
    }
}
