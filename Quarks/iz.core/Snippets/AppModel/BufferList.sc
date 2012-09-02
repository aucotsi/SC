/* IZ Wed 29 August 2012 12:57 PM EEST


Managing lists of buffers. Save the lists to an object archive inside use app support dir. 
Add and remove buffers from a list.
Add, remove, and rename lists.

Buffers are always loaded to the default server. 
Buffers are accessible by the name of the file from which they were loaded, without extension, as a symbol, sending it message 'b'. Example:  

BufferListGui(); // create the gui

BUfferItems from lists are not loaded automatically. Use buttons "load list" or "play" to do that.

Once a BufferItem is loaded, it will reload when the default server reboots.

// To access or play a loaded buffer item: 

\SinedPink.b; // accesses the buffer

\SinedPink.b.play // accesses and plays the buffer

*/


BufferListList : ItemList {
	defaultItemClass { ^BufferList }

	rebuild { array do: _.rebuild }
}

BufferList : ItemList {
	defaultItemClass { ^BufferItem }
	
	add { | item | 
		// reject duplicates: 
		if (array.detect({ | b | b === item}).isNil) { array = array add: item };
	}
	
	rebuild { array = array collect: _.rebuild; }

}

BufferItem : NamedItem {
	// name -> path. item -> Buffer
	// Buffer allocated only and always when server boots or is booted.
	classvar loadingBuffers; // Load buffers only one at a time. See method load.
	classvar <>all;	// IdentityDictionary with one buffer per symbol. 
					// prevent creating duplicate buffers with same path.
	
	var <>nameSymbol;
	*initClass {
		loadingBuffers = IdentityDictionary.new;
		all = IdentityDictionary.new;
		StartUp add: {
			ServerBoot.add({
				Library.at('Buffers') do: _.load;
			}, Server.default);
			ServerQuit.add({
				Library.at('Buffers') do: _.serverQuit;
			}, Server.default);
		}
	}

	*new { | name |
		var nameSymbol, existing;
		nameSymbol = PathName(name).fileNameWithoutExtension.asSymbol;
		(existing = all[nameSymbol]) !? { ^existing };
		^super.new(name).nameSymbol_(nameSymbol);
	}

	rebuild {
		var existing;
		item = nil;
		existing = all[nameSymbol];
		if (existing.notNil) {
			^existing;
		}{
			all[nameSymbol] = this;
			^this;  // (;-)
		}
	}

	init {
		nameSymbol = PathName(name).fileNameWithoutExtension.asSymbol;
	}

	load { | extraAction | // mechanism for loading next buffer after this one is loaded
		item !? { ^this };
		if (Server.default.serverRunning) {
			loadingBuffers[this] = { this.prLoad(extraAction); };
			if (loadingBuffers.size == 1) { this.prLoad(extraAction); }
		}{
			this.storeInLibrary;
			if (Server.default.serverBooting.not) { Server.default.boot };
		};
	}

	prLoad { | extraAction | // called from loadingBuffers when previous buffer is loaded
		Buffer.read(Server.default, name, action: { | buffer |
			item = buffer;
			this.postInfo;
			this.storeInLibrary;
			extraAction.(buffer);
			loadingBuffers[this] = nil;
			loadingBuffers.detect(true).value;
		})	
	}

	serverQuit { item = nil; }

	play {
		item !? { ^item.play };
		this.load({ item.play })
	}

	postInfo { postf("% : % \n", this.minSec, nameSymbol) }
	
	minSec {
		var seconds;
		item ?? { ^"?? min, ?? sec" };
		seconds = item.numFrames / item.sampleRate round: 0.01;
		^format("% min, % sec", seconds / 60 round: 1, seconds % 60);
	}
	
	free {
		var registeredItem;
		registeredItem = Library.at('Buffers', nameSymbol);
		registeredItem !? { if (registeredItem !== this) { ^registeredItem.free }; };
		item !? { item.free; };
		item = nil;
		Library.put('Buffers', this.nameSymbol, nil);
		this.updateLists;
	}

	updateLists { this.class.updateLists; }

	*updateLists {
		var buffers;
		(buffers = Library.at('Buffers')) !? {
			{ this.notify(\bufferList, [Library.at('Buffers').keys.asArray.sort]); }.defer;
		}
	}

	storeInLibrary { 
		Library.put('Buffers', this.nameSymbol, this);
		this.updateLists;
	}

	*openPanel { | doneFunc |
		Dialog.openPanel({ | path | doneFunc.(this.new(path)); });
	}
}

BufferListGui : AppModel0 {
	var listArchivePath = "BufferLists.sctxar";
	var bufferLists, bufList1; // bufList1 gets initialized with defaults if new

	*initClass {
		StartUp add: {
			CocoaMenuItem.add(["Buffers"], { this.new })
		}
	}

	*new {  ^super.new.makeWindow }
	
	makeWindow {
		this.stickyWindow(this.class, \bufferListGui, { | w, app |
			bufferLists = Object.readArchive(Platform.userAppSupportDir +/+ listArchivePath);
			bufferLists !? { bufferLists.rebuild };
			bufferLists = bufferLists ?? { BufferListList("BufferLists.sctxar") };
			if (bufferLists.size == 0) {
				bufferLists.add(Date.getDate.format("Buffer List %c"));
				bufList1 = bufferLists.first;
				(Platform.resourceDir +/+ "sounds/*").pathMatch do: { | path |
					bufList1.add(path);
				};
			};
			w.bounds = Rect(400, 100, 640, 650);

			w.layout = VLayout(
				HLayout(
					StaticText().string_("Lists:"),
					app.button(\bufferLists).getContents(\itemEdit, \append,
						{ | string | string ++ Date.getDate.format(" %c") }
					).view.states_([["add list"]]),
					app.button(\bufferLists).getContents(\itemEdit, \rename)
						.view.states_([["rename list"]]),
					app.button(\bufferLists).getContents(\itemEdit, \delete)
						.view.states_([["delete list"]]),
					Button().states_([["save all"]]).action_({
						app.getAdapter(\bufferLists).adapter.items.save;
					}),
				),
				app.textField(\bufferLists).list
					.name_(\itemEdit).view,
				app.listView(\bufferLists, bufferLists)
					.addAction({ | adapter |
						app.getAdapter(\buffers).adapter.items_(adapter.adapter.item)
					}).view,
			);
			app.windowClosed(w, {
				app.getAdapter(\bufferLists).adapter.items.save;
			})
		})
	}
}


/*

			w.layout = VLayout(
				HLayout(
					StaticText().string_("Lists:"),
					app.button(\bufferLists).getContents(\itemEdit, \append,
						{ | string | string ++ Date.getDate.format(" %c") }
					).view.states_([["add list"]]),
					app.button(\bufferLists).getContents(\itemEdit, \rename)
						.view.states_([["rename list"]]),
					app.button(\bufferLists).getContents(\itemEdit, \delete)
						.view.states_([["delete list"]]),
					Button().states_([["save all"]]).action_({
						app.getAdapter(\bufferLists).adapter.items.save;
					}),
				),
				app.textField(\bufferLists).list
					.name_(\itemEdit).view,
				app.listView(\bufferLists, bufferLists)
					.addAction({ | adapter |
						app.getAdapter(\buffers).adapter.items_(adapter.adapter.item)
					}).view,
				HLayout(
					StaticText().string_("Buffers:"),
					app.button(\buffers, { | widget |
						Dialog.openPanel({ | path |
							widget.adapter.adapter.items add: path;
							widget.adapter.updateListeners;
						});
					}).view.states_([["load new"]]),
					Button().states_([["load all"]]).action_({
						var list;
						list = app.getAdapter(\bufferLists).adapter.item;
						if (list.isNil) {
							"Please choose a buffer list to load first".postln;
						}{
							if (list.size == 0) { "This list contains no buffers".postln; };
							list do: _.load
						}
					}),
					app.button(\buffers, { | widget |
						widget.adapter.adapter.item.load;
					}).view.states_([["load selected"]]),
					app.button(\buffers, { | widget |
						widget.adapter.adapter.item.play;
					}).view.states_([["play"]]),
					app.button(\buffers, { | widget |
						var buffer;
						buffer = widget.adapter.adapter.item;
						buffer.free;
						widget.adapter.adapter remove: buffer;
					}).view.states_([["delete"]]),
					app.button(\buffers, { | widget |
						widget.adapter.adapter.item.free;
					}).view.states_([["free"]]),
				),
				app.listView(\buffers).view.font_(Font.default.size_(10)),
				HLayout(
					VLayout(
						StaticText().string_("Currently loaded buffers:"),
						Button().states_([["load defaults"]]).action_({
							var list;
							list = app.getAdapter(\bufferLists).adapter.item;
							(Platform.resourceDir +/+ "sounds/*").pathMatch do: { | path |
								list.add(path);
							};
							this.updateListeners;
						})
					),
					app.listView(\currentlyLoaded)
						.updateAction_({ | view, sender, adapter |
							view.items = adapter.adapter.items collect: { | b | 
								format("% : %", b, Library.at('Buffers', b).minSec)
							};
						})
						.addNotifier(BufferItem, \bufferList, { | list |
							app.getAdapter(\currentlyLoaded).adapter.items = list;
						})
						.view.font_(Font.default.size_(10)),
				)
			);


*/
