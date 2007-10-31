/* *************************************************************************
 
 IT Mill Toolkit 

 Development of Browser User Interfaces Made Easy

 Copyright (C) 2000-2006 IT Mill Ltd
 
 *************************************************************************

 This product is distributed under commercial license that can be found
 from the product package on license.pdf. Use of this product might 
 require purchasing a commercial license from IT Mill Ltd. For guidelines 
 on usage, see licensing-guidelines.html

 *************************************************************************
 
 For more information, contact:
 
 IT Mill Ltd                           phone: +358 2 4802 7180
 Ruukinkatu 2-4                        fax:   +358 2 4802 7181
 20540, Turku                          email:  info@itmill.com
 Finland                               company www: www.itmill.com
 
 Primary source for information and releases: www.itmill.com

 ********************************************************************** */

package com.itmill.toolkit.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.itmill.toolkit.data.Container;
import com.itmill.toolkit.data.Item;
import com.itmill.toolkit.data.Property;
import com.itmill.toolkit.data.util.IndexedContainer;
import com.itmill.toolkit.terminal.KeyMapper;
import com.itmill.toolkit.terminal.PaintException;
import com.itmill.toolkit.terminal.PaintTarget;
import com.itmill.toolkit.terminal.Resource;

/**
 * <p>
 * A class representing a selection of items the user has selected in a UI. The
 * set of choices is presented as a set of {@link com.itmill.toolkit.data.Item}s
 * in a {@link com.itmill.toolkit.data.Container}.
 * </p>
 * 
 * <p>
 * A <code>Select</code> component may be in single- or multiselect mode.
 * Multiselect mode means that more than one item can be selected
 * simultaneously.
 * </p>
 * 
 * @author IT Mill Ltd.
 * @version
 * @VERSION@
 * @since 3.0
 */
public abstract class AbstractSelect extends AbstractField implements
		Container, Container.Viewer, Container.PropertySetChangeListener,
		Container.PropertySetChangeNotifier, Container.ItemSetChangeNotifier,
		Container.ItemSetChangeListener {

	/**
	 * Item caption mode: Item's ID's <code>String</code> representation is
	 * used as caption.
	 */
	public static final int ITEM_CAPTION_MODE_ID = 0;
	/**
	 * Item caption mode: Item's <code>String</code> representation is used as
	 * caption.
	 */
	public static final int ITEM_CAPTION_MODE_ITEM = 1;
	/**
	 * Item caption mode: Index of the item is used as caption. The index mode
	 * can only be used with the containers implementing the
	 * {@link com.itmill.toolkit.data.Container.Indexed} interface.
	 */
	public static final int ITEM_CAPTION_MODE_INDEX = 2;
	/**
	 * Item caption mode: If an Item has a caption it's used, if not, Item's
	 * ID's <code>String</code> representation is used as caption. <b>This is
	 * the default</b>.
	 */
	public static final int ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID = 3;
	/**
	 * Item caption mode: Captions must be explicitly specified.
	 */
	public static final int ITEM_CAPTION_MODE_EXPLICIT = 4;
	/**
	 * Item caption mode: Only icons are shown, captions are hidden.
	 */
	public static final int ITEM_CAPTION_MODE_ICON_ONLY = 5;
	/**
	 * Item caption mode: Item captions are read from property specified with
	 * <code>setItemCaptionPropertyId</code>.
	 */
	public static final int ITEM_CAPTION_MODE_PROPERTY = 6;

	/**
	 * Interface for option filtering, used to filter options based on user
	 * entered value. The value is matched to the item caption.
	 * <code>FILTERINGMODE_OFF</code> (0) turns the filtering off.
	 * <code>FILTERINGMODE_STARTSWITH</code> (1) matches from the start of the
	 * caption. <code>FILTERINGMODE_CONTAINS</code> (1) matches anywhere in
	 * the caption.
	 */
	public interface Filtering {
		public static final int FILTERINGMODE_OFF = 0;
		public static final int FILTERINGMODE_STARTSWITH = 1;
		public static final int FILTERINGMODE_CONTAINS = 2;

		/**
		 * Sets the option filtering mode.
		 * 
		 * @param filteringMode
		 *            the filtering mode to use
		 */
		public void setFilteringMode(int filteringMode);

		/**
		 * Gets the current filtering mode.
		 * 
		 * @return the filtering mode in use
		 */
		public int getFilteringMode();

	}

	/**
	 * Is the select in multiselect mode?
	 */
	private boolean multiSelect = false;

	/**
	 * Select options.
	 */
	protected Container items;

	/**
	 * Is the user allowed to add new options?
	 */
	private boolean allowNewOptions;

	/**
	 * Keymapper used to map key values.
	 */
	protected KeyMapper itemIdMapper = new KeyMapper();

	/**
	 * Item icons.
	 */
	private final HashMap itemIcons = new HashMap();

	/**
	 * Item captions.
	 */
	private final HashMap itemCaptions = new HashMap();

	/**
	 * Item caption mode.
	 */
	private int itemCaptionMode = ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID;

	/**
	 * Item caption source property id.
	 */
	private Object itemCaptionPropertyId = null;

	/**
	 * Item icon source property id.
	 */
	private Object itemIconPropertyId = null;

	/**
	 * List of property set change event listeners.
	 */
	private LinkedList propertySetEventListeners = null;

	/**
	 * List of item set change event listeners.
	 */
	private LinkedList itemSetEventListeners = null;

	/**
	 * Item id that represents null selection of this select.
	 * 
	 * <p>
	 * Data interface does not support nulls as item ids. Selecting the item
	 * idetified by this id is the same as selecting no items at all. This
	 * setting only affects the single select mode.
	 * </p>
	 */
	private Object nullSelectionItemId = null;

	// Null (empty) selection is enabled by default
	private boolean nullSelectionAllowed = true;

	/* Constructors ********************************************************* */

	/**
	 * Creates an empty Select. The caption is not used.
	 */
	public AbstractSelect() {
		setContainerDataSource(new IndexedContainer());
	}

	/**
	 * Creates an empty Select with caption.
	 */
	public AbstractSelect(String caption) {
		setContainerDataSource(new IndexedContainer());
		setCaption(caption);
	}

	/**
	 * Creates a new select that is connected to a data-source.
	 * 
	 * @param caption
	 *            the Caption of the component.
	 * @param dataSource
	 *            the Container datasource to be selected from by this select.
	 */
	public AbstractSelect(String caption, Container dataSource) {
		setCaption(caption);
		setContainerDataSource(dataSource);
	}

	/**
	 * Creates a new select that is filled from a collection of option values.
	 * 
	 * @param caption
	 *            the Caption of this field.
	 * @param options
	 *            the Collection containing the options.
	 */
	public AbstractSelect(String caption, Collection options) {

		// Creates the options container and add given options to it
		Container c = new IndexedContainer();
		if (options != null) {
			for (Iterator i = options.iterator(); i.hasNext();) {
				c.addItem(i.next());
			}
		}

		setCaption(caption);
		setContainerDataSource(c);
	}

	/* Component methods **************************************************** */

	/**
	 * Paints the content of this component.
	 * 
	 * @param target
	 *            the Paint Event.
	 * @throws PaintException
	 *             if the paint operation failed.
	 */
	public void paintContent(PaintTarget target) throws PaintException {

		// Paints field properties
		super.paintContent(target);

		// Paints select attributes
		if (isMultiSelect()) {
			target.addAttribute("selectmode", "multi");
		}
		if (isNewItemsAllowed()) {
			target.addAttribute("allownewitem", true);
		}
		if (!isNullSelectionAllowed()) {
			target.addAttribute("nullselect", false);
		}

		// Constructs selected keys array
		String[] selectedKeys;
		if (isMultiSelect()) {
			selectedKeys = new String[((Set) getValue()).size()];
		} else {
			selectedKeys = new String[(getValue() == null
					&& getNullSelectionItemId() == null ? 0 : 1)];
		}

		// ==
		// Paints the options and create array of selected id keys
		target.startTag("options");
		int keyIndex = 0;
		// Support for external null selection item id
		Collection ids = getItemIds();
		if (getNullSelectionItemId() != null
				&& (!ids.contains(getNullSelectionItemId()))) {
			// Gets the option attribute values
			Object id = getNullSelectionItemId();
			String key = this.itemIdMapper.key(id);
			String caption = getItemCaption(id);
			Resource icon = getItemIcon(id);
			// Paints option
			target.startTag("so");
			if (icon != null) {
				target.addAttribute("icon", icon);
			}
			target.addAttribute("caption", caption);
			target.addAttribute("nullselection", true);
			target.addAttribute("key", key);
			if (isSelected(id)) {
				target.addAttribute("selected", true);
				selectedKeys[keyIndex++] = key;
			}
			target.endTag("so");
		}

		Iterator i = getItemIds().iterator();
		// Paints the available selection options from data source
		while (i.hasNext()) {
			// Gets the option attribute values
			Object id = i.next();
			String key = this.itemIdMapper.key(id);
			String caption = getItemCaption(id);
			Resource icon = getItemIcon(id); // Paints the option
			target.startTag("so");
			if (icon != null) {
				target.addAttribute("icon", icon);
			}
			target.addAttribute("caption", caption);
			if (id != null && id.equals(getNullSelectionItemId())) {
				target.addAttribute("nullselection", true);
			}
			target.addAttribute("key", key);
			if (isSelected(id) && keyIndex < selectedKeys.length) {
				target.addAttribute("selected", true);
				selectedKeys[keyIndex++] = key;
			}
			target.endTag("so");
		}
		target.endTag("options");
		// ==

		// Paint variables
		target.addVariable(this, "selected", selectedKeys);
		if (isNewItemsAllowed()) {
			target.addVariable(this, "newitem", "");
		}

	}

	/**
	 * Invoked when the value of a variable has changed.
	 * 
	 * @see com.itmill.toolkit.ui.AbstractComponent#changeVariables(java.lang.Object,
	 *      java.util.Map)
	 */
	public void changeVariables(Object source, Map variables) {
		// Try to set the property value

		// New option entered (and it is allowed)
		String newitem = (String) variables.get("newitem");
		if (newitem != null && newitem.length() > 0) {

			// Checks for readonly
			if (isReadOnly()) {
				throw new Property.ReadOnlyException();
			}

			// Adds new option
			if (addItem(newitem) != null) {

				// Sets the caption property, if used
				if (getItemCaptionPropertyId() != null) {
					try {
						getContainerProperty(newitem,
								getItemCaptionPropertyId()).setValue(newitem);
					} catch (Property.ConversionException ignored) {
						// The conversion exception is safely ignored, the
						// caption is
						// just missing
					}
				}
			}
		}

		// Selection change
		if (variables.containsKey("selected")) {
			String[] ka = (String[]) variables.get("selected");

			// Multiselect mode
			if (isMultiSelect()) {

				// TODO Optimize by adding repaintNotNeeded when applicable

				// Converts the key-array to id-set
				LinkedList s = new LinkedList();
				for (int i = 0; i < ka.length; i++) {
					Object id = this.itemIdMapper.get(ka[i]);
					if (id != null && containsId(id)) {
						s.add(id);
					} else if (this.itemIdMapper.isNewIdKey(ka[i])
							&& newitem != null && newitem.length() > 0) {
						s.add(newitem);
					}
				}

				// Limits the deselection to the set of visible items
				// (non-visible items can not be deselected)
				Collection visible = getVisibleItemIds();
				if (visible != null) {
					Set newsel = (Set) getValue();
					if (newsel == null) {
						newsel = new HashSet();
					} else {
						newsel = new HashSet(newsel);
					}
					newsel.removeAll(visible);
					newsel.addAll(s);
					setValue(newsel, true);
				}
			} else {
				// Single select mode
				if (ka.length == 0) {
					// Allows deselection only if the deselected item is visible
					Object current = getValue();
					Collection visible = getVisibleItemIds();
					if (visible != null && visible.contains(current)) {
						setValue(null, true);
					}
				} else {
					Object id = this.itemIdMapper.get(ka[0]);
					if (id != null && id.equals(getNullSelectionItemId())) {
						setValue(null, true);
					} else if (this.itemIdMapper.isNewIdKey(ka[0])) {
						setValue(newitem);
					} else {
						setValue(id, true);
					}
				}
			}
		}
	}

	/**
	 * Gets the component UIDL tag.
	 * 
	 * @return the Component UIDL tag as string.
	 */
	public String getTag() {
		return "select";
	}

	/**
	 * Gets the visible item ids. In Select, this returns list of all item ids,
	 * but can be overriden in subclasses if they paint only part of the items
	 * to the terminal or null if no items is visible.
	 */
	public Collection getVisibleItemIds() {
		if (isVisible()) {
			return getItemIds();
		}
		return null;
	}

	/* Property methods ***************************************************** */

	/**
	 * Returns the type of the property. <code>getValue</code> and
	 * <code>setValue</code> methods must be compatible with this type: one
	 * can safely cast <code>getValue</code> to given type and pass any
	 * variable assignable to this type as a parameter to <code>setValue</code>.
	 * 
	 * @return the Type of the property.
	 */
	public Class getType() {
		if (isMultiSelect()) {
			return Set.class;
		} else {
			return Object.class;
		}
	}

	/**
	 * Gets the selected item id or in multiselect mode a set of selected ids.
	 * 
	 * @see com.itmill.toolkit.ui.AbstractField#getValue()
	 */
	public Object getValue() {
		Object retValue = super.getValue();

		if (isMultiSelect()) {

			// If the return value is not a set
			if (retValue == null) {
				return new HashSet();
			}
			if (retValue instanceof Set) {
				return Collections.unmodifiableSet((Set) retValue);
			} else if (retValue instanceof Collection) {
				return new HashSet((Collection) retValue);
			} else {
				Set s = new HashSet();
				if (this.items.containsId(retValue)) {
					s.add(retValue);
				}
				return s;
			}

		} else {
			return retValue;
		}
	}

	/**
	 * Sets the visible value of the property.
	 * 
	 * <p>
	 * The value of the select is the selected item id. If the select is in
	 * multiselect-mode, the value is a set of selected item keys. In
	 * multiselect mode all collections of id:s can be assigned.
	 * </p>
	 * 
	 * @param newValue
	 *            the New selected item or collection of selected items.
	 * @see com.itmill.toolkit.ui.AbstractField#setValue(java.lang.Object)
	 */
	public void setValue(Object newValue) throws Property.ReadOnlyException,
			Property.ConversionException {
		setValue(newValue, false);
	}

	/**
	 * Sets the visible value of the property.
	 * 
	 * <p>
	 * The value of the select is the selected item id. If the select is in
	 * multiselect-mode, the value is a set of selected item keys. In
	 * multiselect mode all collections of id:s can be assigned.
	 * </p>
	 * 
	 * @param newValue
	 *            the New selected item or collection of selected items.
	 * @param repaintIsNotNeeded
	 *            True if caller is sure that repaint is not needed.
	 * @see com.itmill.toolkit.ui.AbstractField#setValue(java.lang.Object,
	 *      java.lang.Boolean)
	 */
	protected void setValue(Object newValue, boolean repaintIsNotNeeded)
			throws Property.ReadOnlyException, Property.ConversionException {

		if (isMultiSelect()) {
			if (newValue == null) {
				super.setValue(new HashSet(), repaintIsNotNeeded);
			} else if (Collection.class.isAssignableFrom(newValue.getClass())) {
				super.setValue(new HashSet((Collection) newValue),
						repaintIsNotNeeded);
			}
		} else if (newValue == null || this.items.containsId(newValue)) {
			super.setValue(newValue, repaintIsNotNeeded);
		}
	}

	/* Container methods **************************************************** */

	/**
	 * Gets the item from the container with given id. If the container does not
	 * contain the requested item, null is returned.
	 * 
	 * @param itemId
	 *            the item id.
	 * @return the item from the container.
	 */
	public Item getItem(Object itemId) {
		return this.items.getItem(itemId);
	}

	/**
	 * Gets the item Id collection from the container.
	 * 
	 * @return the Collection of item ids.
	 */
	public Collection getItemIds() {
		return this.items.getItemIds();
	}

	/**
	 * Gets the property Id collection from the container.
	 * 
	 * @return the Collection of property ids.
	 */
	public Collection getContainerPropertyIds() {
		return this.items.getContainerPropertyIds();
	}

	/**
	 * Gets the property type.
	 * 
	 * @param propertyId
	 *            the Id identifying the property.
	 * @see com.itmill.toolkit.data.Container#getType(java.lang.Object)
	 */
	public Class getType(Object propertyId) {
		return this.items.getType(propertyId);
	}

	/*
	 * Gets the number of items in the container.
	 * 
	 * @return the Number of items in the container.
	 * 
	 * @see com.itmill.toolkit.data.Container#size()
	 */
	public int size() {
		return this.items.size();
	}

	/**
	 * Tests, if the collection contains an item with given id.
	 * 
	 * @param itemId
	 *            the Id the of item to be tested.
	 */
	public boolean containsId(Object itemId) {
		if (itemId != null) {
			return this.items.containsId(itemId);
		} else {
			return false;
		}
	}

	/**
	 * Gets the Property identified by the given itemId and propertyId from the
	 * Container
	 * 
	 * @see com.itmill.toolkit.data.Container#getContainerProperty(Object,
	 *      Object)
	 */
	public Property getContainerProperty(Object itemId, Object propertyId) {
		return this.items.getContainerProperty(itemId, propertyId);
	}

	/* Container.Managed methods ******************************************** */

	/**
	 * Adds the new property to all items. Adds a property with given id, type
	 * and default value to all items in the container.
	 * 
	 * This functionality is optional. If the function is unsupported, it always
	 * returns false.
	 * 
	 * @return True if the operation succeeded.
	 * @see com.itmill.toolkit.data.Container#addContainerProperty(java.lang.Object,
	 *      java.lang.Class, java.lang.Object)
	 */
	public boolean addContainerProperty(Object propertyId, Class type,
			Object defaultValue) throws UnsupportedOperationException {

		boolean retval = this.items.addContainerProperty(propertyId, type,
				defaultValue);
		if (retval
				&& !(this.items instanceof Container.PropertySetChangeNotifier)) {
			firePropertySetChange();
		}
		return retval;
	}

	/**
	 * Removes all items from the container.
	 * 
	 * This functionality is optional. If the function is unsupported, it always
	 * returns false.
	 * 
	 * @return True if the operation succeeded.
	 * @see com.itmill.toolkit.data.Container#removeAllItems()
	 */
	public boolean removeAllItems() throws UnsupportedOperationException {

		boolean retval = this.items.removeAllItems();
		this.itemIdMapper.removeAll();
		if (retval) {
			setValue(null);
			if (!(this.items instanceof Container.ItemSetChangeNotifier)) {
				fireItemSetChange();
			}
		}
		return retval;
	}

	/**
	 * Creates a new item into container with container managed id. The id of
	 * the created new item is returned. The item can be fetched with getItem()
	 * method. if the creation fails, null is returned.
	 * 
	 * @return the Id of the created item or null in case of failure.
	 * @see com.itmill.toolkit.data.Container#addItem()
	 */
	public Object addItem() throws UnsupportedOperationException {

		Object retval = this.items.addItem();
		if (retval != null
				&& !(this.items instanceof Container.ItemSetChangeNotifier)) {
			fireItemSetChange();
		}
		return retval;
	}

	/**
	 * Create a new item into container. The created new item is returned and
	 * ready for setting property values. if the creation fails, null is
	 * returned. In case the container already contains the item, null is
	 * returned.
	 * 
	 * This functionality is optional. If the function is unsupported, it always
	 * returns null.
	 * 
	 * @param itemId
	 *            the Identification of the item to be created.
	 * @return the Created item with the given id, or null in case of failure.
	 * @see com.itmill.toolkit.data.Container#addItem(java.lang.Object)
	 */
	public Item addItem(Object itemId) throws UnsupportedOperationException {

		Item retval = this.items.addItem(itemId);
		if (retval != null
				&& !(this.items instanceof Container.ItemSetChangeNotifier)) {
			fireItemSetChange();
		}
		return retval;
	}

	/**
	 * Removes the item identified by Id from the container. This functionality
	 * is optional. If the function is not implemented, the functions allways
	 * returns false.
	 * 
	 * @return True if the operation succeeded.
	 * @see com.itmill.toolkit.data.Container#removeItem(java.lang.Object)
	 */
	public boolean removeItem(Object itemId)
			throws UnsupportedOperationException {

		unselect(itemId);
		boolean retval = this.items.removeItem(itemId);
		this.itemIdMapper.remove(itemId);
		if (retval && !(this.items instanceof Container.ItemSetChangeNotifier)) {
			fireItemSetChange();
		}
		return retval;
	}

	/**
	 * Removes the property from all items. Removes a property with given id
	 * from all the items in the container.
	 * 
	 * This functionality is optional. If the function is unsupported, it always
	 * returns false.
	 * 
	 * @return True if the operation succeeded.
	 * @see com.itmill.toolkit.data.Container#removeContainerProperty(java.lang.Object)
	 */
	public boolean removeContainerProperty(Object propertyId)
			throws UnsupportedOperationException {

		boolean retval = this.items.removeContainerProperty(propertyId);
		if (retval
				&& !(this.items instanceof Container.PropertySetChangeNotifier)) {
			firePropertySetChange();
		}
		return retval;
	}

	/* Container.Viewer methods ********************************************* */

	/**
	 * Sets the container as data-source for viewing.
	 * 
	 * @param newDataSource
	 *            the new data source.
	 */
	public void setContainerDataSource(Container newDataSource) {
		if (newDataSource == null) {
			newDataSource = new IndexedContainer();
		}

		if (this.items != newDataSource) {

			// Removes listeners from the old datasource
			if (this.items != null) {
				try {
					((Container.ItemSetChangeNotifier) this.items)
							.removeListener(this);
				} catch (ClassCastException ignored) {
					// Ignored
				}
				try {
					((Container.PropertySetChangeNotifier) this.items)
							.removeListener(this);
				} catch (ClassCastException ignored) {
					// Ignored
				}
			}

			// Assigns new data source
			this.items = newDataSource;

			// Clears itemIdMapper also
			this.itemIdMapper.removeAll();

			// Adds listeners
			if (this.items != null) {
				try {
					((Container.ItemSetChangeNotifier) this.items)
							.addListener(this);
				} catch (ClassCastException ignored) {
					// Ignored
				}
				try {
					((Container.PropertySetChangeNotifier) this.items)
							.addListener(this);
				} catch (ClassCastException ignored) {
					// Ignored
				}
			}

			// TODO: This should be conditional
			fireValueChange(false);
		}
	}

	/**
	 * Gets the viewing data-source container.
	 * 
	 * @see com.itmill.toolkit.data.Container.Viewer#getContainerDataSource()
	 */
	public Container getContainerDataSource() {
		return this.items;
	}

	/* Select attributes **************************************************** */

	/**
	 * Is the select in multiselect mode? In multiselect mode
	 * 
	 * @return the Value of property multiSelect.
	 */
	public boolean isMultiSelect() {
		return this.multiSelect;
	}

	/**
	 * Sets the multiselect mode. Setting multiselect mode false may loose
	 * selection information: if selected items set contains one or more
	 * selected items, only one of the selected items is kept as selected.
	 * 
	 * @param multiSelect
	 *            the New value of property multiSelect.
	 */
	public void setMultiSelect(boolean multiSelect) {

		if (multiSelect != this.multiSelect) {

			// Selection before mode change
			Object oldValue = getValue();

			this.multiSelect = multiSelect;

			// Convert the value type
			if (multiSelect) {
				Set s = new HashSet();
				if (oldValue != null) {
					s.add(oldValue);
				}
				setValue(s);
			} else {
				Set s = (Set) oldValue;
				if (s == null || s.isEmpty()) {
					setValue(null);
				} else {
					// Set the single select to contain only the first
					// selected value in the multiselect
					setValue(s.iterator().next());
				}
			}

			requestRepaint();
		}
	}

	/**
	 * Does the select allow adding new options by the user. If true, the new
	 * options can be added to the Container. The text entered by the user is
	 * used as id. No that data-source must allow adding new items (it must
	 * implement Container.Managed).
	 * 
	 * @return True if additions are allowed.
	 */
	public boolean isNewItemsAllowed() {

		return this.allowNewOptions;
	}

	/**
	 * Enables or disables possibility to add new options by the user.
	 * 
	 * @param allowNewOptions
	 *            the New value of property allowNewOptions.
	 */
	public void setNewItemsAllowed(boolean allowNewOptions) {

		// Only handle change requests
		if (this.allowNewOptions != allowNewOptions) {

			this.allowNewOptions = allowNewOptions;

			requestRepaint();
		}
	}

	/**
	 * Override the caption of an item. Setting caption explicitly overrides id,
	 * item and index captions.
	 * 
	 * @param itemId
	 *            the id of the item to be recaptioned.
	 * @param caption
	 *            the New caption.
	 */
	public void setItemCaption(Object itemId, String caption) {
		if (itemId != null) {
			this.itemCaptions.put(itemId, caption);
			requestRepaint();
		}
	}

	/**
	 * Gets the caption of an item. The caption is generated as specified by the
	 * item caption mode. See <code>setItemCaptionMode()</code> for more
	 * details.
	 * 
	 * @param itemId
	 *            the id of the item to be queried.
	 * @return the caption for specified item.
	 */
	public String getItemCaption(Object itemId) {

		// Null items can not be found
		if (itemId == null) {
			return null;
		}

		String caption = null;

		switch (getItemCaptionMode()) {

		case ITEM_CAPTION_MODE_ID:
			caption = itemId.toString();
			break;

		case ITEM_CAPTION_MODE_INDEX:
			try {
				caption = String.valueOf(((Container.Indexed) this.items)
						.indexOfId(itemId));
			} catch (ClassCastException ignored) {
			}
			break;

		case ITEM_CAPTION_MODE_ITEM:
			Item i = getItem(itemId);
			if (i != null) {
				caption = i.toString();
			}
			break;

		case ITEM_CAPTION_MODE_EXPLICIT:
			caption = (String) this.itemCaptions.get(itemId);
			break;

		case ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID:
			caption = (String) this.itemCaptions.get(itemId);
			if (caption == null) {
				caption = itemId.toString();
			}
			break;

		case ITEM_CAPTION_MODE_PROPERTY:
			Property p = getContainerProperty(itemId,
					getItemCaptionPropertyId());
			if (p != null) {
				caption = p.toString();
			}
			break;
		}

		// All items must have some captions
		return caption != null ? caption : "";
	}

	/**
	 * Sets the icon for an item.
	 * 
	 * @param itemId
	 *            the id of the item to be assigned an icon.
	 * @param icon
	 *            the New icon.
	 */
	public void setItemIcon(Object itemId, Resource icon) {
		if (itemId != null) {
			if (icon == null) {
				this.itemIcons.remove(itemId);
			} else {
				this.itemIcons.put(itemId, icon);
			}
			requestRepaint();
		}
	}

	/**
	 * Gets the item icon.
	 * 
	 * @param itemId
	 *            the id of the item to be assigned an icon.
	 * @return the Icon for the item or null, if not specified.
	 */
	public Resource getItemIcon(Object itemId) {
		Resource explicit = (Resource) this.itemIcons.get(itemId);
		if (explicit != null) {
			return explicit;
		}

		if (getItemIconPropertyId() == null) {
			return null;
		}

		Property ip = getContainerProperty(itemId, getItemIconPropertyId());
		if (ip == null) {
			return null;
		}
		Object icon = ip.getValue();
		if (icon instanceof Resource) {
			return (Resource) icon;
		}

		return null;
	}

	/**
	 * Sets the item caption mode.
	 * 
	 * <p>
	 * The mode can be one of the following ones:
	 * <ul>
	 * <li><code>ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID</code> : Items
	 * Id-objects <code>toString</code> is used as item caption. If caption is
	 * explicitly specified, it overrides the id-caption.
	 * <li><code>ITEM_CAPTION_MODE_ID</code> : Items Id-objects
	 * <code>toString</code> is used as item caption.</li>
	 * <li><code>ITEM_CAPTION_MODE_ITEM</code> : Item-objects
	 * <code>toString</code> is used as item caption.</li>
	 * <li><code>ITEM_CAPTION_MODE_INDEX</code> : The index of the item is
	 * used as item caption. The index mode can only be used with the containers
	 * implementing <code>Container.Indexed</code> interface.</li>
	 * <li><code>ITEM_CAPTION_MODE_EXPLICIT</code> : The item captions must
	 * be explicitly specified.</li>
	 * <li><code>ITEM_CAPTION_MODE_PROPERTY</code> : The item captions are
	 * read from property, that must be specified with
	 * <code>setItemCaptionPropertyId</code>.</li>
	 * </ul>
	 * The <code>ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID</code> is the default
	 * mode.
	 * </p>
	 * 
	 * @param mode
	 *            the One of the modes listed above.
	 */
	public void setItemCaptionMode(int mode) {
		if (ITEM_CAPTION_MODE_ID <= mode && mode <= ITEM_CAPTION_MODE_PROPERTY) {
			this.itemCaptionMode = mode;
			requestRepaint();
		}
	}

	/**
	 * Gets the item caption mode.
	 * 
	 * <p>
	 * The mode can be one of the following ones:
	 * <ul>
	 * <li><code>ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID</code> : Items
	 * Id-objects <code>toString</code> is used as item caption. If caption is
	 * explicitly specified, it overrides the id-caption.
	 * <li><code>ITEM_CAPTION_MODE_ID</code> : Items Id-objects
	 * <code>toString</code> is used as item caption.</li>
	 * <li><code>ITEM_CAPTION_MODE_ITEM</code> : Item-objects
	 * <code>toString</code> is used as item caption.</li>
	 * <li><code>ITEM_CAPTION_MODE_INDEX</code> : The index of the item is
	 * used as item caption. The index mode can only be used with the containers
	 * implementing <code>Container.Indexed</code> interface.</li>
	 * <li><code>ITEM_CAPTION_MODE_EXPLICIT</code> : The item captions must
	 * be explicitly specified.</li>
	 * <li><code>ITEM_CAPTION_MODE_PROPERTY</code> : The item captions are
	 * read from property, that must be specified with
	 * <code>setItemCaptionPropertyId</code>.</li>
	 * </ul>
	 * The <code>ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID</code> is the default
	 * mode.
	 * </p>
	 * 
	 * @return the One of the modes listed above.
	 */
	public int getItemCaptionMode() {
		return this.itemCaptionMode;
	}

	/**
	 * Sets the item caption property.
	 * 
	 * <p>
	 * Setting the id to a existing property implicitly sets the item caption
	 * mode to <code>ITEM_CAPTION_MODE_PROPERTY</code>. If the object is in
	 * <code>ITEM_CAPTION_MODE_PROPERTY</code> mode, setting caption property
	 * id null resets the item caption mode to
	 * <code>ITEM_CAPTION_EXPLICIT_DEFAULTS_ID</code>.
	 * </p>
	 * 
	 * <p>
	 * Setting the property id to null disables this feature. The id is null by
	 * default
	 * </p>.
	 * 
	 * @param propertyId
	 *            the id of the property.
	 * 
	 */
	public void setItemCaptionPropertyId(Object propertyId) {
		if (propertyId != null) {
			this.itemCaptionPropertyId = propertyId;
			setItemCaptionMode(ITEM_CAPTION_MODE_PROPERTY);
			requestRepaint();
		} else {
			this.itemCaptionPropertyId = null;
			if (getItemCaptionMode() == ITEM_CAPTION_MODE_PROPERTY) {
				setItemCaptionMode(ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID);
			}
			requestRepaint();
		}
	}

	/**
	 * Gets the item caption property.
	 * 
	 * @return the Id of the property used as item caption source.
	 */
	public Object getItemCaptionPropertyId() {
		return this.itemCaptionPropertyId;
	}

	/**
	 * Sets the item icon property.
	 * 
	 * <p>
	 * If the property id is set to a valid value, each item is given an icon
	 * got from the given property of the items. The type of the property must
	 * be assignable to Icon.
	 * </p>
	 * 
	 * <p>
	 * Note : The icons set with <code>setItemIcon</code> function override
	 * the icons from the property.
	 * </p>
	 * 
	 * <p>
	 * Setting the property id to null disables this feature. The id is null by
	 * default
	 * </p>.
	 * 
	 * @param propertyId
	 *            the Id of the property that specifies icons for items.
	 */
	public void setItemIconPropertyId(Object propertyId) {
		if ((propertyId != null)
				&& Resource.class.isAssignableFrom(getType(propertyId))) {
			this.itemIconPropertyId = propertyId;
			requestRepaint();
		} else {
			this.itemIconPropertyId = null;
		}
	}

	/**
	 * Gets the item icon property.
	 * 
	 * <p>
	 * If the property id is set to a valid value, each item is given an icon
	 * got from the given property of the items. The type of the property must
	 * be assignable to Icon.
	 * </p>
	 * 
	 * <p>
	 * Note : The icons set with <code>setItemIcon</code> function override
	 * the icons from the property.
	 * </p>
	 * 
	 * <p>
	 * Setting the property id to null disables this feature. The id is null by
	 * default
	 * </p>.
	 * 
	 * @return the Id of the property containing the item icons.
	 */
	public Object getItemIconPropertyId() {
		return this.itemIconPropertyId;
	}

	/**
	 * Tests if an item is selected.
	 * 
	 * <p>
	 * In single select mode testing selection status of the item identified by
	 * {@link #getNullSelectionItemId()} returns true if the value of the
	 * property is null.
	 * </p>
	 * 
	 * @param itemId
	 *            the Id the of the item to be tested.
	 * @see #getNullSelectionItemId()
	 * @see #setNullSelectionItemId(Object)
	 * 
	 */
	public boolean isSelected(Object itemId) {
		if (itemId == null) {
			return false;
		}
		if (isMultiSelect()) {
			return ((Set) getValue()).contains(itemId);
		} else {
			Object value = getValue();
			return itemId.equals(value == null ? getNullSelectionItemId()
					: value);
		}
	}

	/**
	 * Selects an item.
	 * 
	 * <p>
	 * In single select mode selecting item identified by
	 * {@link #getNullSelectionItemId()} sets the value of the property to null.
	 * </p>
	 * 
	 * @param itemId
	 *            the tem to be selected.
	 * @see #getNullSelectionItemId()
	 * @see #setNullSelectionItemId(Object)
	 * 
	 */
	public void select(Object itemId) {
		if (!isSelected(itemId) && this.items.containsId(itemId)) {
			if (isMultiSelect()) {
				Set s = new HashSet((Set) getValue());
				s.add(itemId);
				setValue(s);
			} else if (itemId.equals(getNullSelectionItemId())) {
				setValue(null);
			} else {
				setValue(itemId);
			}
		}
	}

	/**
	 * Unselects an item.
	 * 
	 * @param itemId
	 *            the Item to be unselected.
	 * @see #getNullSelectionItemId()
	 * @see #setNullSelectionItemId(Object)
	 * 
	 */
	public void unselect(Object itemId) {
		if (isSelected(itemId)) {
			if (isMultiSelect()) {
				Set s = new HashSet((Set) getValue());
				s.remove(itemId);
				setValue(s);
			} else {
				setValue(null);
			}
		}
	}

	/**
	 * Notifies this listener that the Containers contents has changed.
	 * 
	 * @see com.itmill.toolkit.data.Container.PropertySetChangeListener#containerPropertySetChange(com.itmill.toolkit.data.Container.PropertySetChangeEvent)
	 */
	public void containerPropertySetChange(
			Container.PropertySetChangeEvent event) {
		firePropertySetChange();
	}

	/**
	 * Adds a new Property set change listener for this Container.
	 * 
	 * @see com.itmill.toolkit.data.Container.PropertySetChangeNotifier#addListener(com.itmill.toolkit.data.Container.PropertySetChangeListener)
	 */
	public void addListener(Container.PropertySetChangeListener listener) {
		if (this.propertySetEventListeners == null) {
			this.propertySetEventListeners = new LinkedList();
		}
		this.propertySetEventListeners.add(listener);
	}

	/**
	 * Removes a previously registered Property set change listener.
	 * 
	 * @see com.itmill.toolkit.data.Container.PropertySetChangeNotifier#removeListener(com.itmill.toolkit.data.Container.PropertySetChangeListener)
	 */
	public void removeListener(Container.PropertySetChangeListener listener) {
		if (this.propertySetEventListeners != null) {
			this.propertySetEventListeners.remove(listener);
			if (this.propertySetEventListeners.isEmpty()) {
				this.propertySetEventListeners = null;
			}
		}
	}

	/**
	 * Adds an Item set change listener for the object.
	 * 
	 * @see com.itmill.toolkit.data.Container.ItemSetChangeNotifier#addListener(com.itmill.toolkit.data.Container.ItemSetChangeListener)
	 */
	public void addListener(Container.ItemSetChangeListener listener) {
		if (this.itemSetEventListeners == null) {
			this.itemSetEventListeners = new LinkedList();
		}
		this.itemSetEventListeners.add(listener);
	}

	/**
	 * Removes the Item set change listener from the object.
	 * 
	 * @see com.itmill.toolkit.data.Container.ItemSetChangeNotifier#removeListener(com.itmill.toolkit.data.Container.ItemSetChangeListener)
	 */
	public void removeListener(Container.ItemSetChangeListener listener) {
		if (this.itemSetEventListeners != null) {
			this.itemSetEventListeners.remove(listener);
			if (this.itemSetEventListeners.isEmpty()) {
				this.itemSetEventListeners = null;
			}
		}
	}

	/**
	 * Lets the listener know a Containers Item set has changed.
	 * 
	 * @see com.itmill.toolkit.data.Container.ItemSetChangeListener#containerItemSetChange(com.itmill.toolkit.data.Container.ItemSetChangeEvent)
	 */
	public void containerItemSetChange(Container.ItemSetChangeEvent event) {
		// Clears the item id mapping table
		this.itemIdMapper.removeAll();

		// Notify all listeners
		fireItemSetChange();
	}

	/**
	 * Fires the property set change event.
	 */
	protected void firePropertySetChange() {
		if (this.propertySetEventListeners != null
				&& !this.propertySetEventListeners.isEmpty()) {
			Container.PropertySetChangeEvent event = new PropertySetChangeEvent();
			Object[] listeners = this.propertySetEventListeners.toArray();
			for (int i = 0; i < listeners.length; i++) {
				((Container.PropertySetChangeListener) listeners[i])
						.containerPropertySetChange(event);
			}
		}
		requestRepaint();
	}

	/**
	 * Fires the item set change event.
	 */
	protected void fireItemSetChange() {
		if (this.itemSetEventListeners != null
				&& !this.itemSetEventListeners.isEmpty()) {
			Container.ItemSetChangeEvent event = new ItemSetChangeEvent();
			Object[] listeners = this.itemSetEventListeners.toArray();
			for (int i = 0; i < listeners.length; i++) {
				((Container.ItemSetChangeListener) listeners[i])
						.containerItemSetChange(event);
			}
		}
		requestRepaint();
	}

	/**
	 * Implementation of item set change event.
	 */
	private class ItemSetChangeEvent implements Container.ItemSetChangeEvent {

		/**
		 * Gets the Property where the event occurred.
		 * 
		 * @see com.itmill.toolkit.data.Container.ItemSetChangeEvent#getContainer()
		 */
		public Container getContainer() {
			return AbstractSelect.this;
		}

	}

	/**
	 * Implementation of property set change event.
	 */
	private class PropertySetChangeEvent implements
			Container.PropertySetChangeEvent {

		/**
		 * Retrieves the Container whose contents have been modified.
		 * 
		 * @see com.itmill.toolkit.data.Container.PropertySetChangeEvent#getContainer()
		 */
		public Container getContainer() {
			return AbstractSelect.this;
		}

	}

	// TODO javadoc
	public void setNullSelectionAllowed(boolean nullSelectionAllowed) {
		this.nullSelectionAllowed = nullSelectionAllowed;
	}

	// TODO javadoc
	public boolean isNullSelectionAllowed() {
		return this.nullSelectionAllowed;
	}

	/**
	 * Returns the item id that represents null value of this select in single
	 * select mode.
	 * 
	 * <p>
	 * Data interface does not support nulls as item ids. Selecting the item
	 * idetified by this id is the same as selecting no items at all. This
	 * setting only affects the single select mode.
	 * </p>
	 * 
	 * @return the Object Null value item id.
	 * @see #setNullSelectionItemId(Object)
	 * @see #isSelected(Object)
	 * @see #select(Object)
	 */
	public final Object getNullSelectionItemId() {
		return this.nullSelectionItemId;
	}

	/**
	 * Sets the item id that represents null value of this select.
	 * 
	 * <p>
	 * Data interface does not support nulls as item ids. Selecting the item
	 * idetified by this id is the same as selecting no items at all. This
	 * setting only affects the single select mode.
	 * </p>
	 * 
	 * @param nullSelectionItemId
	 *            the nullSelectionItemId to set.
	 * @see #getNullSelectionItemId()
	 * @see #isSelected(Object)
	 * @see #select(Object)
	 */
	public void setNullSelectionItemId(Object nullSelectionItemId) {
		this.nullSelectionItemId = nullSelectionItemId;
	}

	/**
	 * Notifies the component that it is connected to an application.
	 * 
	 * @see com.itmill.toolkit.ui.AbstractField#attach()
	 */
	public void attach() {
		super.attach();
	}

	/**
	 * Detaches the component from application.
	 * 
	 * @see com.itmill.toolkit.ui.AbstractComponent#detach()
	 */
	public void detach() {
		super.detach();
	}

}
