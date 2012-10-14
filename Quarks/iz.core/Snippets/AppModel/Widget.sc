/* IZ Fri 31 August 2012  2:57 AM EEST

Redo of Adapter idea from scratch, with radical simplification of principle 

See Value.org file for discussion. 

*/

Widget {
	// NOTE: Model and name are not used in current functionality, but stored for 
	// direct access in possible future applications. 
	// In a "stricter" implementation, model and name variables could be ommitted. 
	var <model;	// the AppModel2 that created me
	var <name;	// the Symbol under which my Value instance is stored in the AppModel
	var <value; 	// the Value instance that I interact with;
	var <view;  	// my view

	*initClass {
		StartUp add: {
			// Allow (proxy-watching) widgets to start and stop watching cmdPeriod notifications
			CmdPeriod add: { CmdPeriod.changed(\cmdPeriod) }
		}
	}

	*new { | model, name, view |
		^this.newCopyArgs(model, name).init(view); // get value and initialize view's onClose action
	}

	init { | argView |
		name !? { value = model.getValue(name); }; // nameless and valueless widgets permitted (?)
		argView !? { this.view = argView; };
	}

	view_ { | argView | // when my view closes, remove all notifications
			view = argView;
			view.onClose = { this.objectClosed }
	}

	do { | action | /* perform a function on self. Used to initialize value etc.
		at creation time, while returning self for further processing */
		action.(this);
	}

	action_ { | action |
		// set my view's action. Pass myself for access to my value etc. 
		view.action = { action.(this) };
	}
	
	/* Set my Value. Remove notification connections from any previous value, and prepare
	   the new value to discnnect if replaced by another one later. Used by prSetControl method.
	   The message notification setup on value must be done separately useing updateAction. */
	value_ { | argValue ... messages |
		this.changed(\disconnect); // Cause previous Value to remove notifications to myself
		value = argValue;
		value.addNotifierOneShot(this, \disconnect, {
			messages do: { | m | this.removeNotifier(value, m); }
		});
	}  
	// set my Value's adapter
	adapter_ { | adapter | value.adapter = adapter; }
	
	updateAction { | message, action | // Add a response to a message from my value
		// Add an action to be done when receiving the specified message from my value-adapter.
		// Pass the sender to the action, to avoid updating self if this is a problem.
		// Also make myself available to the action function.
		this.addNotifier(value, message, { | sender | action.(sender, this) });
	}
	
	updateActionArray { | message, action |
		// Like updateAction, but for passing multiple arguments
		this.addNotifier(value, message, { | ... args | action.(*(args add: this)) });
	}
	
	addUpdateAction { | message, action |
		// add action to be performed when receiving message from value.
		// do not replace any previous action. 
		NotificationCenter.registrations.put(value, message, this,
			NotificationCenter.registrations.at(value, message, this) 
				addFunc: { | ... args | action.(this, *args) } // important: provide access to self
		);
	}
	
	updater { | notifier, message, action |
		// add notifier to self. Provides self as argument to function 
		this.addNotifier(notifier, message, { | ... args | action.(this, *args) })
	}

	// Access to views of values, for actions that require them: 
	viewGetter { | name = \view | // name a widget's view for access by getView
		this.updateAction(name, { | ref  | ref.value = view });
	}

	getView { | name = \view | // get the view of a widget named by viewGetter method
		var ref;
		value.changed(name, ref = `nil)
		^ref.value;
	}

	addValueListener { | listener, message, action |
		// make some other object perform an action whenever receiving a message from my Value
		value.addListener(listener, message, { action.(value) })
	}

	changedAction { | message | // set my view's action to make value send notification message with me
		view.action = { value.changed(message, this) };
	}

	// Initializing behavior for different types of views and functions

	// Numeric value views: NumberBox, Slider, Knob
	simpleNumber { // For NumberBox
		value.adapter ?? { value.adapter = NumberAdapter(value) };
		view.action = { value.adapter.value_(this, view.value) };
		this.updateAction(\number, { | sender |
			if (sender !== this) { view.value = value.adapter.value }
		});
	}

	mappedNumber { | spec | // For Slider, Knob
		value.adapter ?? { value.adapter = NumberAdapter(value) };
		spec !? { value.adapter.spec_(spec) };
		view.action = { value.adapter.standardizedValue_(this, view.value) };
		this.updateAction(\number, { | sender |
			if (sender !== this) { view.value = value.adapter.standardizedValue }
		});
	}

	// String value views
	text {	// For TextField, StaticText 
		value.adapter ?? { value.adapter = TextAdapter(value) };
		view.action = { value.adapter.string_(this, view.string) };
		this.updateAction(\text, { | sender |
			if (sender !== this) { view.string = value.adapter.string }
		});
	}

	textView { | message = \updateText | // for TextView. 
		// Adds updateAction to update text to adapter via button
		this.text;
		this.updateAction(message, { | sender |
			if (sender !== this) { value.adapter.string_(this, view.string) }
		});
	}

	getText { | message = \updateText |
		// Make a button get the text from a TextView and update the Value adapter
		this.notifyAction(message);
	}

	// Special case: get string from view, for further processing, without updating Value
	makeStringGetter { | message = \getString |
		// get the string from a view attached to some Value, without updating the Value itself
		this.updateAction(message, { | stringRef | stringRef.value = view.string; });
	}

	getString { | message = \getString | 
		// Use getString to get the string of a TextView prepared with makeStringGetter
		^value.getString(message);
	}
	
	// ======== Shortcuts (suggested by NC at demo in Leeds ==========
	string_ { | string | value.adapter.string_(this, string); }
	string { ^value.adapter.string }
	// TODO: rename value variable and methods of SpecAdapter ????
	// Name issues: number? magnitude? 
	number_ { | number | value.adapter.value_(this, number); }
	number { ^value.adapter.value }
	standardizedNumber_ { | number | value.adapter.standardizedValue_(this, number); }
	standardizedNumber { ^value.adapter.standardizedValue } // again: naming issues. See value above
	
	// ======== List views: ListView, PopUpMenu =======
	list { | getListAction |
		value.adapter ?? { value.adapter = ListAdapter(value) };
		getListAction = getListAction ?? { { value.adapter.items collect: _.asString } };
		view.action = { value.adapter.index_(this, view.value) };
		this.updateAction(\list, { // | sender |
			view.items = getListAction.(this);
			view.value = value.adapter.index;
		});
		this.updateAction(\index, { // | sender |
			/* if (sender !== this) { */
			view.value = value.adapter.index
			// }
		});
	}

	items_ { | items | value.adapter.items_(this, items); }
	items { ^value.adapter.items }
	item_ { | item | value.adapter.item_(this, item); }
	item { ^value.item }
	index { ^value.adapter.index }
	index_ { | index | value.adapter.index_(this, index) }
	first { value.adapter.first }
	last { value.adapter.last }
	previous { value.adapter.previous }
	next { value.adapter.next }

	listItem { | getItemFunc | // display currently selected item from a list.
		value.adapter ?? { value.adapter = ListAdapter() };
		getItemFunc = getItemFunc ?? { { this.item.asString } }; // TODO: remove defer?
//		this.updateAction(\list, { { view.string = getItemFunc.(this) }.defer(0.2) });
//		this.updateAction(\index, { { view.string = getItemFunc.(this) }.defer(0.2) });
		this.updateAction(\list, { view.string = getItemFunc.(this) });
		this.updateAction(\index, { view.string = getItemFunc.(this) });
		this.replace;		// default action is replace item with your content
	}	

	sublistOf { | valueName, getListFunction | // make me get my list from the item of another list
		value.sublistOf(valueName, getListFunction);
	}

	// Other things to do with Lists: Insert, delete, replace, append, show index, show size, navigate

	// Actions for adding, deleting, replacing items in a list
	replace { | itemCreationFunc | // replace current item in list with an item you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		view.action = { value.adapter.replace(this, itemCreationFunc.(this)); }
	}

	replaceOn { | itemCreationFunc, message = \replace |
		// upon receiving message, replace current item in list with item you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		this.updateAction(message, {
			value.adapter.replace(this, itemCreationFunc.(this));
		});
	}

	append { | itemCreationFunc | // append to list item that you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		view.action = { value.adapter.append(this, itemCreationFunc.(this)); };
	}

	appendOn { | itemCreationFunc, message = \append | // 
		// upon receiving message, append in list the item you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		this.updateAction(message, {
			value.adapter.append(this, itemCreationFunc.(this));
		});
	}

	insert { | itemCreationFunc | // replace current item in list with an item you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		view.action = { value.adapter.insert(this, itemCreationFunc.(this)); };
	}

	insertOn { | itemCreationFunc, message = \insert |
		// upon receiving message, replace current item in list with item you created
		itemCreationFunc = itemCreationFunc ?? { { view.string } };
		this.updateAction(message, {
			value.adapter.insert(this, itemCreationFunc.(this));
		});
	}

	delete { // mostly for buttons
		view.action = { value.adapter.delete(this); };
	}

	// Getting index of current item and size of list
	listIndex { | startAt = 1 | // NumberBox displaying / setting index of element in list
		value.adapter ?? { value.adapter = ListAdapter() };
		view.action = {
			view.value = view.value max: startAt min: (value.adapter.size - 1 + startAt);
			value.adapter.index_(this, view.value - startAt);
		};
		this.updateAction(\list, { | sender |
			if (sender !== this) { view.value = value.adapter.index + startAt }
		});
		this.updateAction(\index, { | sender |
			if (sender !== this) { view.value = value.adapter.index + startAt }
		});
	}

	listSize { // NumberBox displaying number of elements in list (list size). 
		value.adapter ?? { value.adapter = ListAdapter() };
		view.enabled = false;
		this.updateAction(\list, { view.value = value.adapter.size })
	}
	
	// Navigating to different items in list
	firstItem { view.action = { value.adapter.first } }
	lastItem { view.action = { value.adapter.last } }
	previousItem { view.action = { value.adapter.previous } }
	nextItem { view.action = { value.adapter.next } }

	// MultiLevelIdentityDictionary
	dict { | dict |
		value dict: dict;
	}

	branchOf { | superBranch |
		value branchOf: superBranch;
	}

	// NodeProxy stuff
	
	proxyList { | proxySpace | // Auto-updated list for choosing proxy from all proxies in proxySpace
		this.items_((proxySpace ?? { Document.prepareProxySpace }).proxies);
		this.updater(proxySpace, \list, { this.items_(proxySpace.proxies) });
		value.changed(\initProxyControls);	// Initialize proxyWatchers etc. created before me
		[this, thisMethod.name, this.index, this.item, this.items].postln;
	}

	/* Make a button act as play/stop switch for any proxy chosen by another widget from
	a proxy space. The button should be created on the same Value item as the choosing widget.
	The choosing widget is created simply as a listView/popUpmenu on a ProxySpace's proxies. Eg:  
		app.listView(\proxies).items_(proxySpace.proxies).view. 
	Shortcut for listView for choosing proxies: proxyList. */
	proxyWatcher { | playAction, stopAction |
		// Initialize myself only AFTER my proxyList has been created: 
		if (value.adapter.isKindOf(ListAdapter).not) {
			this.addNotifierOneShot(value, \initProxyControls, {
				this.proxyWatcher(playAction, stopAction);
			});
		}{
			playAction ?? { playAction = { this.checkProxy(value.adapter.item.item.play); } };
			stopAction ?? { stopAction = { value.adapter.item.item.stop } };
			view.action = { [stopAction, playAction][view.value].(this) };
			this.addNotifier(CmdPeriod, \cmdPeriod, { view.value = 0 });
			this.updateAction(\list, { this.prStartWatchingProxy(value.adapter.item) });
			this.updateAction(\index, { this.prStartWatchingProxy(value.adapter.item) });
			this.prStartWatchingProxy(value.adapter.item);
		}
	}
	
	prStartWatchingProxy { | proxy |
		// used internally by proxyWatcher method to connect proxy and disconnect previous one
		this.changed(\disconnectProxy);	// remove notifiers to self from previous proxy
		if (proxy.isNil or: { (proxy = proxy.item).isNil } ) { view.value = 0; ^this };
		this.addNotifier(proxy, \play, { view.value = 1 });
		this.addNotifier(proxy, \stop, { view.value = 0 });
		proxy.addNotifierOneShot(this, \disconnectProxy, { // prepare this proxy for removal
			this.removeNotifier(proxy, \play);
			this.removeNotifier(proxy, \stop);
		});
		this.checkProxy(proxy);
	}

	checkProxy { | proxy | // check if proxy is monitoring and update button state
		if (proxy.notNil and: { proxy.isMonitoring }) { view.value = 1 } { view.value = 0 };
		^proxy; // for further use if in another expression.
	}
	
	proxyControlList { | proxyList, autoSelect |
		if (proxyList isKindOf: Symbol) {
			proxyList = model.getValue(proxyList);
		};
		// Initialize myself only AFTER my proxyList has been created: 
		if (proxyList.value.adapter.isNil) {
			this.addNotifierOneShot(proxyList.value, \initProxyControls, {
				this.proxyControlList(proxyList, autoSelect);
			});
		}{
			this.sublistOf(proxyList, { | item |
				item !? { item.specs }
			});
			if (autoSelect.isNil) {
				this.list({ | me |
					me.items collect: { | v | v.adapter.parameter };
				});
			}{
 				this.list({ | me |
					if (autoSelect < me.items.size) {
						me.value.adapter.index_(nil, autoSelect); };
					me.items collect: { | v | v.adapter.parameter.name };
				});
			};
			value.changed(\initProxyControls);	// Initialize proxyControls created before me
		}
	}

	proxyControl {
		var paramList;	// The list of the proxyControlList from which parameters are chosen. 
		// Initialize myself only AFTER my proxyControlList has been created: 
		if (value.adapter isKindOf: NumberAdapter) {
			this.addNotifierOneShot(value, \initProxyControls, { this.proxyControl });
		}{
			// Later my value inst var will be changed. So I keep the paramList in this closure:
			paramList = value.adapter;
			this.listItem({ | me | me.item !? { me.item.adapter.parameter } });
			this.updateAction(\list, { | sender, list |
				paramList.item !? { this.prSetControl(paramList.item); };
			});
			this.updateAction(\index, { | sender, list |
				paramList.item !? { this.prSetControl(paramList.item); };
			});
		}
	}

	prSetControl { | proxyControl |
		this.value_(proxyControl, \number);
		/* provide appropriate view action and updateAction for a numerical value view.
		The view sets these according to its Class.  */
		view.connectToNumberValue(value.adapter, this);
		value.adapter.getValueFromProxy;
	}

	// SoundFile stuff

	soundFileView {
		value.adapter = SoundFileAdapter(value);
		this.updateAction(\read, { | soundfile, startframe, frames |
			view.soundfile = soundfile.soundFile;
			view.read(startframe, frames);
			value.changed(\sfViewAction, this);
		});
		view.mouseUpAction = { | view | value.changed(\sfViewAction, this) };
	}

	// Hiding views
	showOn { | message = \show, show = true |
		// send this to make my view toggle its visibility when notified message + true/false
		this.updateAction(message, { | showP, me | me.view.visible = showP });
		view.visible = show;
	}

	show { | show = true, message = \show |
		/* Sending this from my value hides views of all widgets prepared with showOn */
		value.changed(message, show);
	}

	toggleShow { | message = \show |
		// make a button toggle visibility of other views with its value (0 = hide, 1 = show)
		// other views must be connected with showOn method.
		this.action_({ | me | me.value.changed(message, me.view.value == 1); })
	}

	// add action to existing action function
	// Example: A button prepared with toggleShow adds additional functions to perform when clicked.
	addAction { | action | view.addAction({ action.(this) }); }

	resetOn { | message = \reset, resetValue = 0 |
		// make a button (or other view accepting numeric value) reset to 
		// provided default value when receiving a given message. 
		this.updateAction(message, { | sender, me | me.view.value = resetValue; })
	}

	reset { | message = \reset, resetValue = 0 |
		// send reset notification for any widgets prepared with presetOn
		value.changed(message, resetValue);
	}
}
