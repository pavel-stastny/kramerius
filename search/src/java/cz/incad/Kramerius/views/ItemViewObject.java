package cz.incad.Kramerius.views;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

import cz.incad.Kramerius.exts.menu.context.ContextMenu;
import cz.incad.kramerius.MostDesirable;

public class ItemViewObject {

    public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ItemViewObject.class.getName());
    
    @Inject
    ServletContext servletContext;

    @Inject
    MostDesirable mostDesirable;
    
    @Inject
    HttpServletRequest request;
    
    
    @Inject
    ContextMenu contextMenu;
    
	public ItemViewObject() {
		super();
	}
	
	
	public ContextMenu getContextMenu() {
        return contextMenu;
    }
}