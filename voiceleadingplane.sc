(
var numrowscols=25;
var radius=13;

// define a function to convert a midi note number to a midi note name
var topleftnote, toprightnote, bottomleftnote, bottomrightnote;
var miditoname = ({ arg note = 60, style = \American ;
	var offset = 0 ;
	var midi, notes;
	case { style == \French } { offset = -1}
	{ style == \German } { offset = -3} ;
	midi = (note + 0.5).asInteger;
	notes = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];

	(notes[midi%12] ++ (midi.div(12)-1+offset))
});

// define a function to convert a midi note name to a midi note number
var nametomidi = ({ arg name = "C4", style = \American ;
	var offset = 0 ; // French usage: +1 ; German usage: +3
	var twelves, ones, octaveIndex, midis;

	case { style == \French } { offset = 1}
	{ style == \German } { offset = 3} ;

	midis = Dictionary[($c->0),($d->2),($e->4),($f->5),($g->7),($a->9),($b->11)];
	ones = midis.at(name[0].toLower);

	if( (name[1].isDecDigit), {
		octaveIndex = 1;
	},{
		octaveIndex = 2;
		if( (name[1] == $#) || (name[1].toLower == $s) || (name[1] == $+), {
			ones = ones + 1;
		},{
			if( (name[1] == $b) || (name[1].toLower == $f) || (name[1] == $-), {
				ones = ones - 1;
			});
		});
	});
	twelves = (name.copyRange(octaveIndex, name.size).asInteger) * 12;

	(twelves + 12 + ones + (offset*12))
});



s.waitForBoot({
	var colorlut = [
		Color.new255(255,0,0),
		Color.new255(255,127,0),
		Color.new255(255,255,0),
		Color.new255(0,255,0),
		Color.new255(0,0,255),
		Color.new255(75,0,130),
		Color.new255(143,0,255),
		Color.new255(75,0,130),
		Color.new255(0,0,255),
		Color.new255(0,255,0),
		Color.new255(255,255,0),
		Color.new255(255,127,0),
		Color.new255(255,0,0)
	];
	var center_to_row_col = ();

	// FM Trumpet

	// Realization a Frequency Modulation Trumpet
	// Based on Dodge p.129, an adaption of Dexter Morrill's design

	(
		SynthDef(\fmtrumpet, {arg dur = 1, amp = -3, fundfreq = 440, index = 1, halfsine = 1;
			var env, amp1, amp2, formant, dev1, dev2, rand, vibamp, vibosc, porta, vib, ac1env,
			ac2env, modenv, car1 ,car2, mod, mod2;
			env = EnvGen.kr(Env([0.01, 1, 1, 0.01], [0.01, dur - 0.02, 0.01], \exp), doneAction: 2);
			amp1 = amp.dbamp * 0.7; // amp of the first carrier
			amp2 = amp1 * 0.2; // amp of the second carrier. It is the second carrier
			// that will simulate the formant

			// The equation below is a standard way of calculating a value for frequency
			// which will be as near to a formant frequency as possible while also
			// being a harmonic partial of the fundamental. The important formant
			// for the trumpet is approximately 1500 Hz. Therefore we want to have an
			// increase in energy near that frequency to simulate the formant in order
			// to synthesize a trumpet-like sound. To do that we will use a second carrier
			// oscillator with a frequency corresponding to the formant. The trumpet is an
			// instrument with harmomic partials so the formant must also be a harmonic.
			// For example, suppose the fundamental frequency were 440Hz. It is easy to see
			// that 1500Hz is not a harmonic partial of 440Hz (1500 is not an integer
			// multiple of 440). If we put 440 into the equation:

			//	 formant =  (1500/440).round(1) * 440

			// we get a result of 1320 for formant. The "round" function:

			//	 a.round(b)

			// returns a value for a rounded to the nearest b. So (1500/440).round(1) would
			// return 3 instead of 3.409.
			// We do this because we want to multiply the fundamental by an integer to
			// get a frequency that will be a harmonic partial.
			// We then multiply the fundemental by that integer to get the
			// value of that harmonic in Hz as was done above to get 1320Hz. It might
			// help you understand this better if you insert your own values for
			// fundamentals and formants and do the calculations to see the results!

			formant = (1500 / fundfreq).round(1) * fundfreq;

			// The variable "dev1" is the deviation, or amplitude of the the modulating
			// oscillator. index is an index-like scalar set in the score to make the sound
			// more or less bright.

			dev1 = (fundfreq * 3) * index;

			// The variable dev2 will be used to scale the output of the modulator
			// before being used by the second carrier. Because we don't want to create
			// as many side bands around the second carrier (the formant), the deviation
			// should be smaller.

			dev2 = 0.666 * index;

			// VIBRATO

			// a random component with interpolating LFNoise1

			rand = LFNoise1.ar(15, 0.007);

			// vibrato component amplitude.  This will read from a half sine shape from
			// a buffer stored in memory.  To make sure it only reads once, its frequency
			// will be the reciprocal of the duration.

			vibamp = Osc.kr(halfsine, dur.reciprocal, 0, 0.007);

			// vibrato component main oscillator

			vibosc = SinOsc.ar(5, 0, vibamp);

			// pitch slew function

			porta = EnvGen.kr(Env([0, 0.03, 0.02], [0.06, dur - 0.06], \lin));

			// multiply the three components together with 1 added to
			// create a frequency multiplier centered around 1

			vib	= (1 + rand) * (1 + vibosc) * (1 + porta);

			// ENVELOPES

			ac1env = EnvGen.kr(Env([0.001, 1, 0.8, 0.001], [0.1, dur - 0.25, 0.15], \exp));
			ac2env = EnvGen.kr(Env([0.001, 1, 0.8, 0.001], [0.1, dur - 0.45, 0.3], \exp));
			modenv = EnvGen.kr(Env([0.001, 1, 0.8, 0.001], [0.1, dur - 0.11, 0.01], \exp));

			// MODULATOR

			// The C:M ratio is 1:1, so fm = fc * vibrato

			mod = SinOsc.ar(fundfreq * vib, 0, dev1 * modenv);

			// CARRIER1

			// fc*vib gives the fundamental frequency and then
			// we add the modulator signal

			car1 = SinOsc.ar((fundfreq * vib) + mod, 0, ac1env * amp1);

			// The modulation deviation is scaled back before
			// applying it to the formant carrier, giving
			// fewer sidebands

			mod2 = mod * dev2;

			// The C:M ratio for the formant carrier is different
			// from the fundamental modulator, but since the
			// formant is harmonic to fc, the resulting formant:M
			// ratio is also harmonic, and in the reduced ratio
			// M will still be 1 -- so the same spectral components
			// will be present.

			// CARRIER2

			car2 = SinOsc.ar((formant * vib) + mod2, 0, amp2 * ac2env);

			Out.ar(0, (car1 + car2) * env!2);
		}).load(s);
	);

	s.sync;

	~i1 = Synth(\fmtrumpet, [\amp, -20, \dur, 1000, \fundfreq, 0]);
	~i2 = Synth(\fmtrumpet, [\amp, -20, \dur, 1000, \fundfreq, 0]);

	(
		w = Window.new(bounds:Rect(200,200,1000,1000));
		w.view.background_(Color.white);
		v = UserView(w, w.view.bounds.insetBy(5,5));
		w.acceptsMouseOver_(true);
		topleftnote = TextField();
		bottomleftnote = TextField();
		toprightnote= TextField();
		bottomrightnote = TextField();
		v.layout = VLayout(
			HLayout(
				GridLayout.rows(
					[[topleftnote.string_("C5").align_(\center), columns:2],
						[toprightnote.string_("C5").align_(\center), columns:2], nil],
					[[bottomleftnote.string_("C3").align_(\center), columns:2],
						[bottomrightnote.string_("C3").align_(\center), columns:2],nil],
			), nil),
			nil
		);

		v.drawFunc = { |v|
			Pen.use {
				Pen.translate(0,v.bounds.height/2);
				Pen.rotate(-pi/4);
				numrowscols.do({ | row |
					numrowscols.do({
						| col |

						var x = col.linlin(-1,numrowscols*sqrt(2),0,v.bounds.width);
						var y = row.linlin(-1,numrowscols*sqrt(2),0,v.bounds.width);
						var color = colorlut[(row-col)%12];
						var m11,m12,m21,m22,trx,try;
						Pen.fillColor = color;
						Pen.strokeColor = Color.black;
						a = Pen.addArc(x@y, radius, 0, 2pi);
						m = a.matrix;
						m11 = m[0];
						m21 = m[1];
						m12 = m[2];
						m22 = m[3];
						trx = m[4];
						try = m[5];
						Pen.fillStroke;

						// map from user coordinates x,y to screen coordinates using system matrix
						center_to_row_col.put( row*numrowscols + col, [(((m11*x)+(m12*y)+trx)), (((m21*x)+(m22*y)+try))]);

					});
				});
			};
		};
		v.mouseDownAction = {
			| view, x, y, modifiers, buttonNumber, clickCount |
			var found = False;
			center_to_row_col.keysValuesDo({
				| key, value |
				var distancesquared = (((value[0]-x)*(value[0]-x)) + ((value[1]-y)*(value[1]-y)));
				var radiussquared = radius*radius;
				if ((distancesquared < radiussquared) && (found == False))
				{
					var row = key.div(numrowscols);
					var col = key%numrowscols;
					var rownote = row.linlin(0,
						24,
						nametomidi.value(bottomleftnote.string),
						nametomidi.value(toprightnote.string));
					var colnote = col.linlin(0,
						24,
						nametomidi.value(bottomrightnote.string),
						nametomidi.value(topleftnote.string));
					found = True;
					("row: "++row++"col: "++col).postln;
					~i1.set(\fundfreq, rownote.midicps);
					~i2.set(\fundfreq, colnote.midicps);
				};
			});
			if (found == False)
			{
				~i1.set(\fundfreq, 0);
				~i2.set(\fundfreq, 0);
			};
		};

		v.onClose = {
			~i1.free;
			~i2.free;
		};

		w.front;
	);

});

)