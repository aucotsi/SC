/* IZ 2011 0317

managing a group of synths which outputs audio on the 43 channels of the Klangdom 

Things to provide: 

- Global fade-in, fade-out, volume control. 
- Volume control for individual channels
- ... other utils such as: 
  - Creating the array of 43 synths by iterating a function given as parameter
  - Accessing the array of 43 synths
  - Volume control with x-y PanAz rings on the 43 channels of the dome
  - Adding effects
  - Adding control bus arrays that algorithmically control an array of parameters
  - Adding a standard kdpanvol or knpanvolw panner 
  - Controlling the kdpanvol panner. 
 
The kdpanvol panner should be a KDpan kr instance. 

*/