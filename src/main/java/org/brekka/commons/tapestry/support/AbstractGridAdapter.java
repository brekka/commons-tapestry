/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brekka.commons.tapestry.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry5.beaneditor.PropertyModel;
import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.brekka.commons.persistence.model.ListingCriteria;
import org.brekka.commons.persistence.model.OrderByPart;
import org.brekka.commons.persistence.model.OrderByProperty;

/**
 * Bridges the Tapestry GridDataSource with service methods.
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
public abstract class AbstractGridAdapter<T> implements GridDataSource {

    private final Class<T> rowType;
    
    private List<T> rows;
    
    private int offsetIndex = 0;
    
    /**
     * 
     */
    public AbstractGridAdapter(Class<T> rowType) {
        this.rowType = rowType;
    }

    
    protected abstract int estimateRowCount();
    
    protected abstract List<OrderByPart> defaultSort();

    protected abstract List<T> retrieveInRange(ListingCriteria listingCriteria);
    
    protected void enhance(T row) {
        // By default does nothing
    }

    
    /* (non-Javadoc)
     * @see org.apache.tapestry5.grid.GridDataSource#getAvailableRows()
     */
    @Override
    public final int getAvailableRows() {
        return estimateRowCount();
    }
    
    /* (non-Javadoc)
     * @see org.apache.tapestry5.grid.GridDataSource#prepare(int, int, java.util.List)
     */
    @Override
    public final void prepare(int startIndex, int endIndex, List<SortConstraint> sortConstraints) {
        offsetIndex = startIndex;
        List<OrderByPart> orderParts = new ArrayList<OrderByPart>(sortConstraints.size());
        for (SortConstraint sortConstraint : sortConstraints) {
            PropertyModel propertyModel = sortConstraint.getPropertyModel();
            String property = propertyModel.getPropertyName();
            boolean ascending = true;
            if (sortConstraint.getColumnSort() == ColumnSort.UNSORTED) {
                // ignore
                continue;
            }
            ascending = sortConstraint.getColumnSort() == ColumnSort.ASCENDING;
            orderParts.add(new OrderByProperty(property, ascending));
        }
        if (orderParts.isEmpty()) {
            orderParts = defaultSort();
        }
        rows = retrieveInRange(new ListingCriteria(startIndex, endIndex, orderParts));
    }
    


    /* (non-Javadoc)
     * @see org.apache.tapestry5.grid.GridDataSource#getRowValue(int)
     */
    @Override
    public final T getRowValue(int index) {
        index = index - offsetIndex;
        if (index >= rows.size()) {
            return null;
        }
        T row = rows.get(index);
        enhance(row);
        return row;
    }
    
    

    /* (non-Javadoc)
     * @see org.apache.tapestry5.grid.GridDataSource#getRowType()
     */
    @Override
    public Class<T> getRowType() {
        return rowType;
    }

}
