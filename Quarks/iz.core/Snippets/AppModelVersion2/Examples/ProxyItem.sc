/* IZ Wed 05 September 2012 12:01 PM BST

Redoing proxy containers for proxy history and spec lists.

*/

NamedItem { // Not used. Draft. 
	var <>name, <>item;

	*new { | name, item | ^this.newCopyArgs(name, item) }

	asString { ^name }
	== { | item |
		if (item.isNil) {
			^true
		}{
			^name == item.name
		}
	}
}

ProxyItem : NamedItem {
	var <history, <specs;
	
	*new { | name, item | ^this.newCopyArgs(name, item).init }
	
	init {
		history = List.new;
		specs = List.new;
		item !? { this.addNotifier(item, \proxySpecs, { | specs | this.specs = specs }) };
	}
	
	addSnippet { | snippet |
		history.add(snippet);
		history.notify(\list);
	}

	specs_ { | argSpecs |
		specs.array = argSpecs;
		specs.notify(\list);
	}
}