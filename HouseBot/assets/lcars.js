/*
LCARS Buttons taken from LCARS Framework jQuery Plugin v2.0 (FRAGMENT!)
Requires Jquery 1.4.*
@author: Josh Messer
@date: 1.26.2011
The original code is here: http://lcars.ws/lcarsdist/
This is only a fragment of the original code!
*/
(function($) {
 $.fn.lcarsButton = function(options) { // set defaults
	var defaults = {
		rounded: 'both', // accepts both, left, right, none
		extended: false, // this is true or false
		color: 'orange',
		subTitle: { // The sub title for your button
			direction: 'none', // left or right
			text: '' // the text for the sub title
		},
		blank: 'none', // blank button? left / right / none
	};
	var options = $.extend(defaults, options);
		
		// here we get the class for are rounded button
		var button_rounded = [];
		button_rounded["both"] = "RR";
		button_rounded["right"] = "SR";
		button_rounded["left"] = "RS";
		button_rounded['none'] = "";
		options.rounded = button_rounded[options.rounded];
		
		if( options.extended == true){
			var _extend = 'L';
		}else{
			var _extend = '';
		}
		
		var subtitle_dir = [];
		subtitle_dir['left'] = 'B_titleL';
		subtitle_dir['right'] = 'B_titleR';
		subtitle_dir['none'] = undefined;
		options.subTitle.direction = subtitle_dir[options.subTitle.direction];
		if( options.subTitle.text == undefined ){
			options.subTitle.text = '';
		}
		
		var blank_dir = [];
		blank_dir['none'] = undefined;
		blank_dir['left'] = 'B_blankL';
		blank_dir['right'] = 'B_blankR';
		options.blank = blank_dir[options.blank];
		
		
		this.each(function(){
			var _this = $(this);
		if(!_this.hasClass('button')){
			_this.addClass('button');
		}
		
		_this.addClass(options.rounded).addClass(_extend).css('background-color',lcars_colors(options.color));
		
		if( options.subTitle.direction != '' || options.subTitle.direction != undefined ){
			_this.prepend('<span class="'+options.subTitle.direction+'" style="color:'+lcars_colors(options.color)+'">'+options.subTitle.text+'</span>');
		}
		
			
		if( options.blank == '' || options.blank == undefined ){
		}else{
			if( _this.find('span').length >= 1){
				_this.removeClass(options.rounded).find('span').removeClass(options.subTitle.direction).addClass(options.blank).html('');
			}
			if( _this.find('span').length == 0){
				_this.removeClass(options.rounded);
				_this.prepend('<span class="'+options.blank+'"></span>');
			}
			
			if(options.blank == 'B_blankL'){
				_this.addClass('RS');
			}
			if(options.blank == 'B_blankR'){
				_this.addClass('SR');
			}
		}
		
		});
		
}

})(jQuery);

// http://www.lcars47.com/p/lcars-101.html
// Last column, Star Trek: Nemesis colors
// Modeling color after this picture from  Star Trek: Nemesis:
// http://upload.wikimedia.org/wikipedia/en/4/4b/Enterprise-E_LCARS.jpg
// The LCARS jQuery system uses ten colors, but there are only 8 in the pallet
// Also, I'm resorting these to match the list
// I am also using #FF9900 for some text and buttons as seenin the above image,
// Or at least I think I do, at least it looks good to me.
function lcars_colors(input_color){
	var lcars_colors = [];
	lcars_colors["white"] = "#CCDDFF";
	lcars_colors["lightBlue"] = "#5599FF";
	lcars_colors["lightTan"] = "#3366FF"; // Less Light Blue
	lcars_colors["pink"] = "#cc6699"; // Extra Color, from TNG later series (2nd column)
	lcars_colors["lightRed"] = "#cc6666"; // Extra Color, from TNG later serids (2nd column)
	lcars_colors["blue"] = "#0011EE";
	lcars_colors["purple"] = "#000088";
	lcars_colors["tan"] = "#BBAA55";
	lcars_colors["orange"] = "#BB4411";
	lcars_colors["red"] = "#882211";
	color = lcars_colors[input_color];
	if(color == undefined){
		return input_color;
	}else{
		return color;
	}
}