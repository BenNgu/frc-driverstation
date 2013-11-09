package org.anidev.frcds.pc.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;
import javax.swing.plaf.TabbedPaneUI;

public class DraggableTabbedPane extends JTabbedPane {
	public static final int DETACH_THRESHOLD=5;
	private volatile boolean dragging=false;
	private volatile Rectangle tabBounds=null;
	private volatile BufferedImage tabImage=null;
	private volatile Point currentMouseLocation=null;
	private volatile int draggedTabIndex=0;
	private volatile DropState dropState=null;
	private volatile JWindow dragWindow=null;
	private Map<Integer,Boolean> draggingEnabled=Collections
			.synchronizedMap(new HashMap<Integer,Boolean>());
	private volatile int indicatorPos=-1;
	private List<Listener> listeners=Collections
			.synchronizedList(new ArrayList<Listener>());

	public DraggableTabbedPane() {
		super();
		addMouseMotionListener(new DragMotionListener());

		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if(!dragging) {
					return;
				}
				dragging=false;
				tabImage=null;
				tabBounds=null;
				dragWindow.setVisible(false);
				dragWindow.dispose();
				dragWindow=null;
				boolean moved=false;
				int insertPos=0;
				if(indicatorPos>=0&&indicatorPos!=draggedTabIndex
						&&indicatorPos-1!=draggedTabIndex) {
					Component comp=getComponentAt(draggedTabIndex);
					String title=getTitleAt(draggedTabIndex);
					Icon icon=getIconAt(draggedTabIndex);
					String tooltip=getToolTipTextAt(draggedTabIndex);
					removeTabAt(draggedTabIndex);
					insertPos=indicatorPos;
					if(draggedTabIndex<indicatorPos) {
						insertPos--;
					}
					insertTab(title,icon,comp,tooltip,insertPos);
					setSelectedIndex(insertPos);
					moved=true;
				}
				synchronized(listeners) {
					for(Listener listener:listeners) {
						// Also canceled if ROW_MOVE but did not
						// actually move to a different location
						if(dropState==null||dropState==DropState.NO_CHANGE
								||(dropState==DropState.ROW_MOVE&&!moved)) {
							listener.tabDropCanceled(draggedTabIndex,e);
						} else if(dropState==DropState.ROW_MOVE&&moved) {
							listener.tabMoved(draggedTabIndex,insertPos,e);
						} else if(dropState==DropState.DETACHED) {
							listener.tabDetached(draggedTabIndex,e);
						}
					}
				}
				indicatorPos=-1;
				repaint();
			}
		});
	}

	public DropState calcTabDropState(int index,int x,int y) {
		if(x>=0&&x<=getWidth()&&y>=tabBounds.y&&y<=tabBounds.y+tabBounds.height) {
			return DropState.ROW_MOVE;
		}
		if(Math.sqrt(Math.pow(x-tabBounds.x,2)+Math.pow(y-tabBounds.y,2))>DETACH_THRESHOLD) {
			return DropState.DETACHED;
		}
		return DropState.NO_CHANGE;
	}

	public void setTabDraggable(int index,boolean draggable) {
		draggingEnabled.put(index,draggable);
	}

	public boolean isTabDraggable(int index) {
		return draggingEnabled.get(index);
	}

	public void addTabDragListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeTabDragListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(indicatorPos<0||!dragging) {
			return;
		}
		int x,y,h;
		Rectangle hoverBounds=getUI().getTabBounds(DraggableTabbedPane.this,
				(indicatorPos>=getTabCount()?indicatorPos-1:indicatorPos));
		x=hoverBounds.x;
		y=hoverBounds.y;
		h=hoverBounds.height;
		if(indicatorPos>=getTabCount()) {
			x+=hoverBounds.width;
		}
		g.setColor(new Color(0,102,255));
		g.fillRect(x-1,y,3,h);
	}

	public static abstract class Listener {
		public void tabDragging(int index,MouseEvent e) {
		}

		public void tabDropCanceled(int index,MouseEvent e) {
		}

		public void tabMoved(int oldIndex,int newIndex,MouseEvent e) {
		}

		public void tabDetached(int index,MouseEvent e) {
		}
	}

	public static enum DropState {
		NO_CHANGE, // tab dropped back where it came from
		ROW_MOVE, // tab dropped elsewhere on the tabstrip
		DETACHED; // tab dropped far enough away to be detached
	}

	private class DragMotionListener extends MouseMotionAdapter {
		public void mouseDragged(MouseEvent e) {
			updateStatus(e);
			updateImage(e);
			int oldPos=indicatorPos;
			updateIndicator(e);
			if(oldPos!=indicatorPos) {
				repaint();
			}
		}

		public void updateStatus(MouseEvent e) {
			if(!dragging) {
				// Gets the tab index based on the mouse position
				int tabNumber=getUI().tabForCoordinate(
						DraggableTabbedPane.this,e.getX(),e.getY());
				if(tabNumber>=0) {
					draggedTabIndex=tabNumber;
					tabBounds=getUI().getTabBounds(DraggableTabbedPane.this,
							tabNumber);
					dragging=true;
					synchronized(listeners) {
						for(Listener listener:listeners) {
							listener.tabDragging(draggedTabIndex,e);
						}
					}
				}
			}
		}

		public void updateImage(MouseEvent e) {
			if(dragging) {
				// Paint the tabbed pane to a buffer
				BufferedImage totalImage=new BufferedImage(getWidth(),
						getHeight(),BufferedImage.TYPE_INT_ARGB);
				Graphics2D totalGraphics=totalImage.createGraphics();
				totalGraphics.setClip(tabBounds);
				// Don't be double buffered when painting to a static
				// image.
				setDoubleBuffered(false);
				paintComponent(totalGraphics);

				// Paint just the dragged tab to the buffer
				tabImage=new BufferedImage(tabBounds.width,tabBounds.height,
						BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics=tabImage.createGraphics();
				graphics.drawImage(totalImage,0,0,tabBounds.width,
						tabBounds.height,tabBounds.x,tabBounds.y,tabBounds.x
								+tabBounds.width,tabBounds.y+tabBounds.height,
						DraggableTabbedPane.this);
				currentMouseLocation=new Point(e.getXOnScreen(),e
						.getYOnScreen());

				if(dragWindow==null) {
					dragWindow=new JWindow() {
						@Override
						public void paint(Graphics g) {
							g.drawImage(tabImage,0,0,null);
						}
					};
					dragWindow.setSize(tabBounds.getSize());
					dragWindow.setVisible(true);
				}
				dragWindow.setLocation(currentMouseLocation);
				dragWindow.repaint();
			}
		}

		public void updateIndicator(MouseEvent e) {
			if(!dragging) {
				indicatorPos=-1;
				return;
			}
			int x=e.getX();
			int y=e.getY();
			dropState=calcTabDropState(draggedTabIndex,x,y);
			indicatorPos=-1;
			if(dropState!=DropState.ROW_MOVE) {
				return;
			}
			TabbedPaneUI ui=getUI();
			int hoverTabIndex=ui.tabForCoordinate(DraggableTabbedPane.this,x,y);
			if(hoverTabIndex<0) {
				Rectangle endTabBounds=ui.getTabBounds(
						DraggableTabbedPane.this,getTabCount()-1);
				if(x>endTabBounds.x+endTabBounds.width) {
					indicatorPos=getTabCount();
				}
				return;
			}
			Rectangle hoverBounds=ui.getTabBounds(DraggableTabbedPane.this,
					hoverTabIndex);
			indicatorPos=hoverTabIndex;
			if(x>=hoverBounds.x+hoverBounds.width/2.0) {
				indicatorPos++;
			}
		}
	}
}
