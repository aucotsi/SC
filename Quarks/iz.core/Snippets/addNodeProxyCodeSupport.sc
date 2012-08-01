/* IZ 2012 07 29


Make ProxyCode command-shift-w only perform <proxy>.source = <interpreted snippet object> when the object returned by the interpreted snippet code is one of the supported NodeProxy sources as listed in the help for NodeProxy, with one modification: NodeProxy is not accepted as source for NodeProxy, to prevent such node-proxy clones from being generated accidentally. 

*/

+ Object  { isValidProxyCode { ^false } }
+ NodeProxy {
	isValidProxyCode { ^false } // !!! if you really want this, enter it using shift-return
	controls {
		
	}
} 
+ Function { isValidProxyCode { ^true } }
+ SimpleNumber { isValidProxyCode { ^true } }
+ Bus { isValidProxyCode { ^true } }
+ SynthDef { isValidProxyCode { ^true } }
+ Symbol { isValidProxyCode { ^true } }
+ Pattern { isValidProxyCode { ^true } }
+ Stream { isValidProxyCode { ^true } }
+ Nil  { isValidProxyCode { ^true } }
+ Pdef { isValidProxyCode { ^true } } 
+ EventPatternProxy { isValidProxyCode { ^true } }
+ Task { isValidProxyCode { ^true } }
+ Tdef { isValidProxyCode { ^true } }
+ Event { isValidProxyCode { ^true } }
+ Association { isValidProxyCode { ^true } }
// + AbstractPlayer { isValidProxyCode { ^true } }
// + Instr { isValidProxyCode { ^true } }