/*!
 * jQuery Peepshow
 * Copyright 2011 ZULIUS LLC All rights reserved. 
 *
 * jQuery plugin for creating an image gallery. The plugin centers/crops/expands groups of images
 * contained in unordered list elements <li>.
 *
 * Demo: http://demos.zulius.com/jquery/peepshow
 *
 * Dev notes:
 * - An image group is simply a set of images contained within a list element <li>.
 * - Groups of images are wrapped with a relatively positioned image-wrap div for cropping/centering purposes.
 * - Auto centering wraps the entire jq-peepshow element with a relatively positioned
 *   div.jq-peepshow-auto-center so floated elements can be centered.
 * - Hover events are bound to the image-wrap div's. Public methods 'expand' and 'shrink'
 *   can be called on either the image-wrap div's or their parent li's.
 * - Expansion/shrinkage is done to the image-wrap div's.
 * - Once the image-wrap div has been added to the DOM, the plugin assumes
 *   it has no child images of class "exclude". 
 *
 *
 * @fileOverview jQuery Peepshow 
 * @author ZULIUS LLC 
 * @version 1.2.2
 */
(function ($) {
    var galleryCount = 0,


        /**
         * Global data object. Only used when only 1 peepshow gallery
         * is being displayed. Otherwise, data/options are stashed per gallery
         * with $(container).data();
         *
         * @type Object
         */
            data = {},

        /**
         * Class added to each jq-peepshow gallery container.
         *
         * @type String
         */
            containerClass = 'jq-peepshow',

        /**
         * Public methods the plugin exposes.
         *
         * @type Object
         */
            methods = {

            /**
             * Initializes the peepshow image gallery.
             *
             * @this {element} gallery container
             * @public
             * @param {Object} options The options object passed to the plugin.
             * @see jQuery.fn.peepshow.defaults
             */
            init:function (options) {
                var $container = this;
                var _data = {
                    opt:$.extend({}, jQuery.fn.peepshow.defaults, options),

                    // Flag that plugin specific images have been preloaded
                    pluginImagesPreloaded:false,

                    /**
                     * interval object. Contains id and data regarding the
                     * image rotation interval.
                     * @type Object
                     */
                    interval:{
                        id:null,
                        length:null,
                        firstImageLength:null
                    },

                    /*
                     * Object that contains info about touch mode.
                     */
                    touch:{
                        /*** on/off flag ***/
                        enabled:false,

                        /**
                         * Currently expanded image wrap
                         */
                        $expanded:undefined,

                        /*** Timeout id for fading an expanded img's top overlay ***/
                        overlayFadeTimeoutId:undefined,

                        /**
                         * Flag that the device orientation just changed. Used to distinugish
                         * between status bar auto-scroll and orientation change when viewport
                         * is at the top of the page.
                         */
                        orientationTriggered:false,


                        /** flag if this is an iOS device **/
                        ios:false,

                        /** ios major version **/
                        iosv:undefined
                    },
                };

                _validateOpt(_data);

                $container.addClass(containerClass);

                // enable/disable touch mode
                _touchInit(_data, $container);

                // center image gallery
                if (_data.opt.autoCenter) {
                    $container.wrap($('<div></div>').addClass('jq-peepshow-auto-center'));
                }

                // set auto margins
                if (_data.opt.autoMargin) {
                    _autoMargin(this, _data);
                }

                // determines the # of image groups/row based on container size
                if (typeof(_data.opt.itemsPerRow) == 'string' && _data.opt.itemsPerRow.toLowerCase() == 'auto') {
                    _autoRows(this, _data);
                }

                // set the initial rotation interval. Take into account the interval for hoverIntent
                // so that the first image is rotated slightly faster than the others.
                _setFirstImageRotationSpeed(_data);

                // loop over all galleries
                return this.each(function () {
                    galleryCount++;

                    var $this = $(this),
                        rowItemCount = 0,
                        $lis = $(this).children('ul').children('li');

                    // stash the _data object per gallery
                    $this.data('jq-peepshow', _data);

                    if (galleryCount == 1) {
                        data = _data;
                    }
                    else {
                        data = {};
                    }

                    $lis.each(function () {
                        // 1. make rows
                        rowItemCount++;
                        if (parseInt(_data.opt.itemsPerRow)) {
                            rowItemCount = _layoutRows(this, rowItemCount, _data.opt);
                        }

                        // 2. group & crop images
                        _crop(this);

                        // 3. bind load/error events to gallery images
                        var $images = $(this).find('img').not(_data.opt.excludeSelector);
                        _bindLoad($images);

                        // 4. preload plugin specific images
                        _preloadPluginImages(this, _data);

                        // 5. bind hover/tap events
                        _bindExpand(this, _data);
                    });
                });
            },

            /**
             * Expands and displays the first image in an image group.
             *
             * @this {element} Should be either the parent li element or the parent opt.wrapClass
             *                 element of an image group.
             * @public
             */
            expand:function () {
                var _data = _getData(this);

                // touch mode
                if (_data.touch.enabled) {
                    return _touchExpand.call(this);
                }

                var $this = $(this),
                    $imgWrap = $this.hasClass(_data.opt.wrapClass) ? $this : $this.find('.' + _data.opt.wrapClass);
                if ($imgWrap.data('expanded')) return;

                var $img = $($imgWrap.find('img').not('[thumb]')[0]),
                    $thumb = $($imgWrap.find('img[thumb]').first()),
                    imgW = $img.width(),
                    imgH = $img.height(),
                    expandDim = _applyExpansionLimits(imgW, imgH, _data.opt);

                // if there's a custom thumbnail, hide it, show the next image
                if ($thumb.length) {
                    $thumb.hide();
                    $img.show().css({opacity:1.0});
                }

                // expand it
                $imgWrap.stop(true, true);

                var expandArgs = {
                    'margin-left':'-' + ( (parseInt(expandDim[0]) / 2) ) + 'px',
                    'margin-top':'-' + ( (parseInt(expandDim[1]) / 2) ) + 'px',
                    'width':expandDim[0],
                    'height':expandDim[1],
                    'top':'50%',
                    'left':'50%'
                };

                _setGroupMaxDim($imgWrap, expandArgs.width, expandArgs.height);

                _showOverlay({
                    imgWrap:$imgWrap,
                    img:$img
                });

                function _doneGrown(el, imgEl) {
                    $(el).data('expanded', true)
                        .parent('li').addClass('expanded');
                    $(imgEl).addClass('shown');
                }

                // immediate expansion
                if (!_data.opt.expandSpeed) {
                    $imgWrap.css(expandArgs);
                    _doneGrown($imgWrap, $img);
                    return;
                }

                // animated expansion
                $imgWrap.animate(expandArgs,
                    {
                        duration:_data.opt.expandSpeed,
                        easing:_data.opt.expandEasing,
                        complete:function (e) {
                            _doneGrown(this, $img);
                        }
                    }
                );
            },


            /**
             * Shrinks an expanded image and displays the cropped first image in an image group.
             *
             * @this {element} Should be either the parent li element or the parent opt.wrapClass
             *                 element of an image group.
             * @public
             *
             */
            shrink:function () {
                _unrotate(this);
            },

            /**
             * Removes added elements, element data and unbinds all events associated with the peepshow gallery.
             *
             * @this {element} Should be a DOM element of class "jq-peepshow"
             * @public
             */
            destroy:function () {
                var _data = _getData(this);

                // auto center wrap
                if (_data.opt.autoCenter) {
                    this.unwrap();
                }

                // touch mode bindings
                if (_data.touch.enabled) {
                    _touchShrinkUnbind(_data);
                }

                return this.find('.' + _data.opt.wrapClass).each(function () {
                    var $this = $(this);

                    // hover events
                    $this.unbind('mousemove' + _data.opt.hoverIntentOptions.bindNamespace + ' ' +
                        'mouseenter' + _data.opt.hoverIntentOptions.bindNamespace + ' ' +
                        'mouseleave' + _data.opt.hoverIntentOptions.bindNamespace);

                    // data
                    $this.removeData('expanded')
                        .removeData('displayedImageIndex')
                        .removeData('rotateState')
                        .removeData('maxDim');

                    // image load events
                    $this.find('.' + _data.opt.wrapClass)
                        .unbind('load ' +
                        'error ' +
                        'load' + _data.opt.bindNamespace + '-center');

                    // pause/play
                    $this.find('.rotate-state-button')
                        .unbind('click' + _data.opt.bindNamespace);
                });
            }
        };

    /*******************/
    /* private methods */
    /*******************/

    /**
     * Initializes touch mode (if enabled or autodetect).
     *
     * @param {object} The data object
     * @param {element} jQuery object of the gallery container
     * @private
     * @return void
     */
    function _touchInit(_data, $container) {
        switch (_data.opt.touchMode) {
            case false:
                _data.touch.enabled = false;
                break;
            case true:
                _data.touch.enabled = true;
                break;
            default:
                if (typeof(_data.opt.touchMode) == 'string' && _data.opt.touchMode.toLowerCase() == 'auto') {
                    _data.touch.enabled = 'ontouchstart' in window ? true : false;
                }
                break;
        }

        if (!_data.touch.enabled) {
            return;
        }

        // hide url bar
        setTimeout(function () {
            window.scrollTo(0, 1)
        }, 100);

        // detect ios
        var ua = window.navigator.userAgent;
        if (ua.match(/iphone/i) || ua.match(/ipod/i) || ua.match(/ipad/i)) {
            _data.touch.ios = true;

            if (/CPU like Mac OS X/i.test(navigator.userAgent)) {
                _data.touch.iosv = 1;
            }
            else {
                // regex out the major version number
                var match;
                if ((match = /OS (\d)_\d(_\d)? like Mac OS X/i.exec(ua)) !== null) {
                    _data.touch.iosv = parseInt(match[1]);
                }
            }
        }

        $container.addClass('touch');
    }

    /**
     * Validates options, updates any options that are invalid.
     *
     * @param {object} _data The data object for this gallery
     * @private
     */
    function _validateOpt(_data) {
        if (typeof(_data.opt.itemsPerRow) == 'number') {
            _data.opt.itemsPerRow = parseInt(_data.opt.itemsPerRow);
        }

        _data.opt.crop[0] = _css2num(_data.opt.crop[0]);
        _data.opt.crop[1] = _css2num(_data.opt.crop[1]);

        _data.opt.minExpansion[0] = _css2num(_data.opt.minExpansion[0]);
        _data.opt.minExpansion[1] = _css2num(_data.opt.minExpansion[1]);
        _data.opt.maxExpansion[0] = _css2num(_data.opt.maxExpansion[0]);
        _data.opt.maxExpansion[1] = _css2num(_data.opt.maxExpansion[1]);

        _data.opt.autoMarginRatio = parseFloat(_data.opt.autoMarginRatio);

        _data.opt.rotateSpeed = parseInt(_data.opt.rotateSpeed);

        // ensure minExpansion/maxExpansion is >= crop
        if (_data.opt.crop[0] > _data.opt.minExpansion[0]) {
            _data.opt.minExpansion[0] = _data.opt.crop[0];
        }

        if (_data.opt.crop[1] > _data.opt.minExpansion[1]) {
            _data.opt.minExpansion[1] = _data.opt.crop[1];
        }

        if (_data.opt.crop[0] > _data.opt.maxExpansion[0]) {
            _data.opt.maxExpansion[0] = _data.opt.crop[0];
        }

        if (_data.opt.crop[1] > _data.opt.maxExpansion[1]) {
            _data.opt.maxExpansion[1] = _data.opt.crop[1];
        }

    }

    /**
     * Returns the interger value of a css value (ie. 12px -> 12 )
     *
     * @param {String} css string value
     * @private
     * @return {Int} int value
     */
    function _css2num(value) {
        return parseInt(value, 10) || 0;
    }

    /**
     * Saves the data object for a gallery.
     *
     * @param el Any element within a peepshow gallery
     * @param _data Data object
     * @access protected
     * @return void
     */
    function _saveData(el, _data) {
        // if there's more than 1 gallery, lookup
        // the data from the gallery's container object
        if (galleryCount > 1) {
            var $el = $(el);
            if (!$el.hasClass(containerClass)) {
                $el = $el.parents('.' + containerClass).first();
            }

            $el.data('jq-peepshow', _data);
            return;
        }

        // just one gallery, so just use the global variable ref
        data = _data;
    }

    /**
     * Retrieves the data object for a gallery.
     *
     * @param el Any element within a peepshow gallery
     * @access protected
     * @return {object}
     */
    function _getData(el) {
        // if there's more than 1 gallery, lookup
        // the data from the gallery's container object
        if (galleryCount > 1) {
            var $el = $(el);
            if (!$el.hasClass(containerClass)) {
                $el = $el.parents('.' + containerClass).first();
            }

            var _data = $el.data('jq-peepshow');
            return _data;
        }

        // just one gallery, so just use the global variable ref 
        return data;
    }

    /**
     * Determines the number of image groups that can
     * horizontally fit in a row. Updates data.opt.itemsPerRow
     * accordingly.
     *
     * @param {Object} container the 'jq-peepshow' container div
     * @param {object _data The data object
        * @private
     * @return {Boolean} true if the autoRows determination was successful,
     *                   false if it couldn't figure it out
     */
    function _autoRows(container, _data) {
        var containerParentWidth = $(container).parent().width(),
            $childLi = $(container).children('ul').children('li'),
            totalLiWidth = 0,
            avgLiWidth = 0,
            itemsPerRow = 0;

        if (!$childLi.length) {
            return false;
        }

        $childLi.each(function () {
            var $this = $(this);
            totalLiWidth += _data.opt.crop[0] +
                _css2num($this.css('margin-right')) +
                _css2num($this.css('margin-left')) +
                _css2num($this.css('padding-right')) +
                _css2num($this.css('padding-left'));
        });


        // get the average li width
        avgLiWidth = totalLiWidth / $childLi.length;
        if (avgLiWidth <= 0) {
            return false;
        }


        itemsPerRow = Math.floor(containerParentWidth / avgLiWidth);
        if (itemsPerRow <= 0) {
            return false;
        }

        _data.opt.itemsPerRow = itemsPerRow;
        return true;
    }

    /**
     * Determines appropriate right and bottom margins of each image
     * group li element. Uses data.opt.autoMarginRatio to determine margins
     * as a ratio of the image crop size. Also uses data.opt.itemsPerRow and the jq-peepshow container.
     *
     * @param {Object} container the 'jq-peepshow' container div
     * @param {object} The data object
     * @private
     * @return {Boolean} true if the autoMargin determination was successful,
     *                   false if it couldn't figure it out
     */
    function _autoMargin(container, _data) {
        var containerParentWidth = $(container).parent().width(),
            $childLi = $(container).children('ul').children('li'),
            rowLiWidth = 0,
            marginRight = 0,
            marginBottom = 0;

        if (!_data.opt.autoMarginRatio) {
            return false;
        }

        // margin-bottom
        if (_data.opt.crop[1]) {
            marginBottom = parseInt(_data.opt.crop[1]) * parseFloat(_data.opt.autoMarginRatio);
        }

        // margin-right
        if (_data.opt.crop[0]) {
            marginRight = parseInt(_data.opt.crop[0]) * parseFloat(_data.opt.autoMarginRatio);

            // specified itemsPerRow, so make margins fit in container
            if (parseInt(_data.opt.itemsPerRow) && _data.opt.itemsPerRow > 0) {
                // get total width for a row
                for (var i = 0; i < _data.opt.itemsPerRow; i++) {
                    if (!$childLi[i]) {
                        continue;
                    }

                    var $li = $($childLi[i]);

                    rowLiWidth += parseInt(_data.opt.crop[0]) +
                        _css2num($li.css('padding-left')) +
                        _css2num($li.css('padding-right'));
                }

                // Reduce the marginRight if the row width exceeds the containers width.
                // Remember, last item in row will have class="last" ---> margin-right = 0.
                var marginRightRow = marginRight * (_data.opt.itemsPerRow - 1);
                rowLiWidth += marginRightRow;

                if (rowLiWidth > containerParentWidth) {
                    marginRight = (marginRightRow - (rowLiWidth - containerParentWidth))
                        / (_data.opt.itemsPerRow - 1);

                    // minimum margin-right is 1px
                    if (marginRight < 1) {
                        marginRight = 1;
                    }

                    // make margin-bottom even with new margin-right
                    marginBottom = marginRight;
                }
            }

        }


        // apply margin to li elements
        if (marginRight) {
            $childLi.css('margin-right', marginRight);
        }
        if (marginBottom) {
            $childLi.css('margin-bottom', marginBottom);
        }
    }

    /**
     * Floats containing li elements to create rows.
     *
     * @param {Object} el li element
     * @param {Int} rowItemCount number of li elements per row
     * @param {Object} opt Options object
     * @private
     * @return {Int}
     */
    function _layoutRows(el, rowItemCount, opt) {

        if (rowItemCount == 1 || (rowItemCount == (opt.itemsPerRow + 1))) {
            $(el).addClass('first');
        }

        if (rowItemCount == opt.itemsPerRow) {
            $(el).addClass('last');
            rowItemCount = 0;
        }

        return rowItemCount;

    }

    /**
     * Updates the displayed current image count (ie. 1/4 -> 2/4)
     *
     * @param {Object} arg.current
     * @param {Object} arg.total
     * @param {Object} arg.imgWrap
     * @param {object} _data
     * @private
     */
    function _touchUpdateOverlayCount(arg, _data) {
        arg = $.extend({}, {
            current:undefined,
            total:undefined,
            imgWrap:_data.touch.$expanded
        }, arg);

        var $overlayTop = arg.imgWrap.find('.overlay.top:first');

        // lookup current image index
        if (typeof(arg.current) == 'undefined') {
            arg.current = arg.imgWrap.data('humanStep');
            if (typeof(arg.current) == 'undefined') {
                arg.current = 1;
            }
        }

        // lookup image count
        if (typeof(arg.total) == 'undefined') {
            arg.total = arg.imgWrap.find('img').not('[thumb]').length;
        }

        var countText = arg.current + '/' + arg.total;

        arg.imgWrap.find('.overlay.top:first .count:first').text(countText);
    }

    /**
     * Displays a translucent overlay over an expanded image.
     *
     * @param {Object} arg.imgWrap          The image-wrap div
     * @param {Object} arg.img              The currently displayed image element
     * @param {Integer} arg.fadeOutTopAfter 0 = do not fade out the top overlay
     *                                      X = # of millseconds to wait to fade out top overlay
     * @param {Boolean} arg.forceBottom     Show the bottom overlay regardless of caption text existence.
     Defaults to opt.playPauseButton.
     * @param {Mixed} arg.showTop           (touch mode only)
     *                                      true   = show the top overlay
     *                                      false  = do not show the top overlay
     * @private
     */
    function _showOverlay(arg) { //imgWrap, img, force){
        arg = $.extend({},
            {
                showTop:false,
                forceBottom:undefined,
                img:undefined,
                imgWrap:undefined,
                fadeOutTopAfter:2000
            }, arg);

        var $imgWrap = $(arg.imgWrap),
            _data = _getData($imgWrap);

        if (arg.forceBottom == undefined) {
            arg.forceBottom = _data.opt.playPauseButton;
        }

        if (arg.img == undefined) {
            var displayedImageIndex = $imgWrap.data('displayedImageIndex');
            arg.img = $($imgWrap.find('img')[displayedImageIndex]);
        }

        var $overlayTop = $imgWrap.children('.overlay.top')
        $overlayBottom = $imgWrap.children('.overlay.bottom'),
            text = $(arg.img).attr(_data.opt.captionAttribute);

        // don't display bottom overlay if caption attribute is missing
        // and playPauseButton is disabled 
        if (!arg.forceBottom && (!text || typeof(text) != 'string' || !text.length)) {
            if ($overlayBottom.is(':visible')) {
                $overlayBottom.fadeOut('fast');
            }
        }

        // display overlay bottom
        else {
            $overlayBottom.children('span').text(text);

            // the ridiculous things that make IE work
            if ($.browser.msie) {
                $overlayBottom.css('filter', $overlayBottom.css('filter'));
            }

            // fade in
            if (!$overlayBottom.is(':visible')) {
                $overlayBottom.fadeIn(_getTransitionSpeed($imgWrap));
            }
        }

        // touch mode - display overlay top 
        if (_data.touch.enabled && arg.showTop) {
            clearTimeout(_data.touch.overlayFadeTimeoutId);
            $overlayTop.fadeIn(_getTransitionSpeed($imgWrap));

            // fade out top overlay 
            if (arg.fadeOutTopAfter) {
                _data.touch.overlayFadeTimeoutId = setTimeout(function () {
                    if ($overlayTop.is(':visible')) {
                        $overlayTop.fadeOut('fast');
                    }
                }, arg.fadeOutTopAfter);
                _saveData($imgWrap, _data);
            }
        }
    }

    /**
     * Hides the translucent image overlay
     *
     * @param {Object} imgWrap The image-wrap div
     * @private
     */
    function _hideOverlay(imgWrap) {
        $(imgWrap).children('.overlay').hide();
    }

    /**
     * Binds the hover/touch events to the image-wrap element
     *
     * @param {Object} liEl The parent li elements
     * @param {Object} data object
     * @priveate
     */
    function _bindExpand(liEl, _data) {
        if (_data.opt.bindNamespace) {
            _data.opt.hoverIntentOptions.bindNamespace = _data.opt.bindNamespace + '-hover';
        }

        _data.opt.hoverIntentOptions.over = function (e) {
            $(this).peepshow('expand');
            _rotate(this);
        };
        _data.opt.hoverIntentOptions.out = function (e) {
            $(this).peepshow('shrink');
        };

        // bind events
        return $(liEl).children('.' + _data.opt.wrapClass).each(function () {
            // imgWrap
            var $imgWrap = $(this);

            // no images? ignore it
            if (!$imgWrap.find('img').length) {
                return;
            }

            if (_data.touch.enabled) {

                // find any anchors wrapping imgs
                var $anchors = $imgWrap.find('a').has('img').not(_data.opt.excludeSelector);

                $anchors.each(function () {
                    // get bound events
                    var $this = $(this),
                        clickEvents = [],
                        boundEvents = $this.data('events'),
                        onclick = $this.attr('onclick');

                    // save/remove the onclick attribute
                    if (onclick) {
                        $this.data('onclickEvent', onclick);
                        //clickEvents.push(eval("(function(){" + onclick +"})"));
                        $this.removeAttr('onclick');
                    }

                    if (boundEvents && boundEvents['click']) {
                        // Loop through each click event bound to $this control
                        $.each(events['click'], function () {
                            clickEvents.push(this);
                        });

                        // Remove all click handlers
                        $this.unbind('click');
                    }

                    if (!clickEvents.length) {
                        return;
                    }

                    // save click events
                    $this.data('boundClickEvents', clickEvents);
                });

                // bind image wrap expansion
                $imgWrap.bind('click' + _data.opt.bindNamespace, function (e) {
                    // prevent going to anchor's href
                    if (e != undefined) {
                        e.preventDefault();
                    }

                    if ($(this).data('expanded')) {
                        return;
                    }

                    _touchExpand.call(this);
                    _rotate(this);
                    return false;
                });
                return;
            }

            // expand image boundaries using hoverIntent
            $imgWrap.hoverIntent(_data.opt.hoverIntentOptions);
        });
    }

    /**
     * Set the amount of time to display the first image after it's expanded.
     * Display the 1st image for slightly less time than the remaining images.
     * This helps create the effect that all images are displayed
     * for an equal amount of time.
     *
     * Operates on the global interval object.
     *
     * @param {Object} opt Options object
     * @private
     */
    function _setFirstImageRotationSpeed(_data) {
        var ratio = 2.5,
            firstImageInterval = parseInt(_data.opt.rotateSpeed - (ratio * _data.opt.hoverIntentOptions.interval));

        _data.interval.firstImageLength = firstImageInterval > _data.opt.hoverIntentOptions.interval ?
            firstImageInterval : _data.opt.rotateSpeed;
    }

    /**
     * Displays the next image in the image group, hides the currently
     * displayed image.
     *
     * @param {Object} e Either the parent li or image-wrap element
     * @this {Object} Either the parent li or image-wrap element
     * @private
     */
    function _rotate($imgWrap) {
        var $this = $(this),
            $imgWrap = $($imgWrap),
            _data = _getData($imgWrap);

        $imgWrap = $imgWrap.hasClass(_data.opt.wrapClass) ? $imgWrap : $imgWrap.find('.' + _data.opt.wrapClass);

        var $allImages = $imgWrap.find('img');

        // is interval already running? sometimes. this safeguards against onmouseleave misfires (*ahem* IE)
        if (_data.interval.id != null) {
            _unrotate($imgWrap);
            return;
        }

        // 0 or 1 image, don't rotate
        if ($allImages.length < 2) {
            return;
        }

        _setRotateState($imgWrap, 'play', $imgWrap.children('.overlay').children('.rotate-state-button'));

        _data.interval.length = _data.interval.firstImageLength;

        _data.interval.id = setInterval(function () {
            // check if rotation is paused
            if (_getRotateState($imgWrap) == 'pause') {
                return;
            }

            // change interval on 1st (img loaded) run
            if (_data.interval.length == _data.interval.firstImageLength) {
                _data.interval.length = parseInt(_data.opt.rotateSpeed);
                clearTimeout(_data.interval.id);
                _data.interval.id = setInterval(arguments.callee, _data.interval.length);
                _saveData($imgWrap, _data);
            }

            // gather elements and current index
            var index = $imgWrap.data('displayedImageIndex'),
                nextIndex,
                $shown,
                $next,
                humanStep = $imgWrap.data('humanStep');

            if (index == undefined) {
                index = 0;
            }

            $shown = $($allImages[index]);

            // if this is a thumbnail, skip it, display the next image
            if (!!$shown.attr('thumb')) {
                index = ($allImages[index + 1] == undefined) ? 0 : index + 1;
                $shown = $($allImages[index]);

                // fail: only 1 image and it's a thumb?
                if (!!$shown.attr('thumb')) {
                    return;
                }

                $imgWrap.data('displayedImageIndex', index);
            }

            nextIndex = ($allImages[index + 1] == undefined) ? 0 : index + 1;
            $next = $($allImages[nextIndex]);

            // if next image is a thumbnail, skip it
            if (!!$next.attr('thumb')) {
                nextIndex = ($allImages[nextIndex + 1] == undefined) ? 0 : nextIndex + 1;
                $next = $($allImages[nextIndex]);
            }

            // did we loop?
            if (nextIndex < index) {
                humanStep = 1;
            }
            else {
                humanStep++;
            }

            // next image hasn't loaded yet
            if (!$next.attr('complete')) {
                if (_data.opt.showLoader && (_data.opt.playPauseButton)) {
                    _showNextLoader($imgWrap);
                    return;
                }
            }

            $shown.addClass('last-shown')

            $next.css({opacity:0.0, display:'inline'})
                .addClass('shown');

            $imgWrap.data({
                'displayedImageIndex':nextIndex,
                'humanStep':humanStep
            });

            if (_data.touch.enabled) {
                _touchUpdateOverlayCount({
                        imgWrap:$imgWrap
                    },
                    _data);

                _showOverlay({
                    imgWrap:$imgWrap,
                    fadeOutTopAfter:0,
                    img:$next,
                    showTop:false
                });

                _touchPositionExpanded({
                        imgWrap:$imgWrap,
                        fade:false,
                        callback:function () {
                            _transitionImages($shown, $next, $imgWrap);
                        }

                    },
                    _data);
            }
            else {
                _showOverlay({
                    imgWrap:$imgWrap,
                    img:$next
                });
                _expandResize($imgWrap, $next, function () {
                    _transitionImages($shown, $next, $imgWrap);
                });
            }

        }, _data.interval.length);

        _saveData($imgWrap, _data);
    }

    function _transitionImages($shown, $next, $imgWrap) {
        var _data = _getData($imgWrap);

        // no transition effect
        if (!_data.opt.transitionEffect ||
            (typeof(_data.opt.transitionEffect) != 'string') ||
            _data.opt.transitionEffect.toLowerCase() == 'none'
            ) {
            $shown.css({opacity:0.0})
                .removeClass('shown last-shown');
            $next.css({opacity:1.0});

            return;
        }

        var speed = _getTransitionSpeed($imgWrap);

        switch (_data.opt.transition) {
            case 'fade':
            default:
                $next.animate({opacity:1.0},
                    speed,
                    function () {
                        $shown.removeClass('shown last-shown');
                    });

                // Fade out shown image.  next image may be smaller,
                // so this will prevent shown from showing in the background
                $shown.animate({opacity:0.0}, speed);
                break;
        }
    }

    /**
     * Setter for the current state of rotation.
     *
     * @param {Object} imgWrap image-wrap element
     * @param {String} state Rotation state: 'pause' or 'play'
     * @param {Object} button The clicked button element
     * @private
     */
    function _setRotateState(imgWrap, state, button) {
        $(imgWrap).data('rotateState', state);

        if (button != undefined) {
            if (state == 'pause') {
                $(button).addClass('paused');
                return;
            }

            $(button).removeClass('paused');
        }
    }

    /**
     * Getter for current rotation state
     *
     * @param {Object} imgWrap image-wrap element
     * @private
     */
    function _getRotateState(imgWrap) {
        return $(imgWrap).data('rotateState');
    }

    /**
     * Switches rotation state from play -> pause, and visa versa
     *
     * @param {Object} imgWrap image-wrap element
     * @param {Object} button The clicked button element
     * @private
     */
    function _flipRotateState(imgWrap, button) {
        if (_getRotateState(imgWrap) == 'play') {
            _setRotateState(imgWrap, 'pause', button);
            return;
        }

        _setRotateState(imgWrap, 'play', button);

    }

    /**
     * Returns the rotation transition speed in milliseconds.
     *
     * @param {Object} jquery element within the gallery
     * @private
     * @return {Int}
     */
    function _getTransitionSpeed($el) {
        var speed = 300,
            _data = _getData($el || this);

        if (_data.opt.transitionEffectSpeedRatio && _data.opt.rotateSpeed) {
            speed = _data.opt.transitionEffectSpeedRatio * _data.opt.rotateSpeed;
        }
        ;

        return speed;
    }

    /**
     * Shrinks an expanded image and displays the cropped first image in an image group.
     *
     * @param {Object} e Either the parent li or image-wrap element
     * @this {Object} Either the parent li or image-wrap element
     * @private
     */
    function _unrotate($imgWrap) {
        var $this = (this),
            $imgWrap = $($imgWrap),
            _data = _getData($imgWrap);

        $imgWrap = $imgWrap.hasClass(_data.opt.wrapClass) ? $imgWrap : $imgWrap.find('.' + _data.opt.wrapClass);

        clearInterval(_data.interval.id);
        _data.interval.id = null;

        // get all the images in the wrap and turn down their opacity 
        var $allImages = $imgWrap.find('img');
        $imgWrap.stop(true, true);
        $allImages.stop(false, true)
            .removeClass('last-shown shown')
            .css({opacity:0.0});

        // performed on the img that is the first child
        var $shown = $allImages.first();
        $shown.css({opacity:1.0,
            display:'inline',
            'z-index':1       // necessary for IE
        });

        $imgWrap.data('displayedImageIndex', 0);

        _shrink($imgWrap);
    }

    /**
     * Shrinks an expanded image
     *
     * @param {Object} imgWrap image-wrap div
     * @private
     */
    function _shrink(imgWrap) {
        var $imgWrap = $(imgWrap),
            _data = _getData($imgWrap);

        if (!$imgWrap.data('expanded')) {
            return;
        }

        var shrinkArgs = {
            position:'absolute',
            left:0,
            top:0,
            width:'100%',
            height:'100%',
            'margin-top':0,
            'margin-left':0
        };

        $imgWrap.stop(true, true);

        if (_data.touch.enabled) {
            var $imgs = $imgWrap.find('img');
            $imgs.each(function () {
                var $img = $(this),
                    origDim = $img.data('origDim');

                $img.removeClass('shown');

                // restore image original size
                if (origDim != undefined && origDim.length == 2) {
                    $img.css({width:origDim[0], height:origDim[1]});
                }
            });

            $imgWrap.css(shrinkArgs);
            _center($imgs[0]);
            _touchShrinkUnbind.call($imgWrap, _data);
        }
        else if (!_data.opt.shrinkSpeed) {
            $imgWrap.css(shrinkArgs)
                .find('img').removeClass('shown');
        }
        else {
            $imgWrap.stop(true, true)
                .animate(shrinkArgs,
                {
                    duration:_data.opt.shrinkSpeed,
                    easing:_data.opt.shrinkEasing,
                    complete:function () {
                        $(this).find('img').removeClass('shown');
                    }
                });
        }

        _hideOverlay(imgWrap);

        $imgWrap.data({expanded:false, displayedImageIndex:0})
            .removeData('maxDim')
            .parent('li').removeClass('expanded');
    }

    /**
     * Sets the maximum expanded dimensions for an image group.
     *
     * @param {Object} imgWrap image-wrap element
     * @param {Number} width
     * @param {Number} heigh
     * @private
     */
    function _setGroupMaxDim(imgWrap, width, height) {
        var $imgWrap = $(imgWrap),
            maxDim = $imgWrap.data('maxDim'),
            updateMaxDim = false;


        if (maxDim == undefined) {

            $imgWrap.data('maxDim', [width, height]);
            return;
        }

        if (width > maxDim[0]) {
            maxDim[0] = width;
            updateMaxDim = true;
        }

        if (height > maxDim[1]) {
            maxDim[1] = height;
            updateMaxDim = true;
        }

        if (!updateMaxDim) {
            return;
        }

        $imgWrap.data('maxDim', maxDim);
    }

    /**
     * Preloads plugin specific images
     *
     * @param {Object} container li element
     * @param {Object} opt Options object
     * @private
     */
    function _preloadPluginImages(el, _data) {
        if (_data.pluginImagesPreloaded) {
            return false;
        }
        var preload = [],
            $imgWrap = $(el).children('.' + _data.opt.wrapClass).first(),
            spinnerSrc = $imgWrap.css('background-image');

        if (spinnerSrc) {
            spinnerSrc = spinnerSrc.replace(/"/g, "").replace(/url\(|\)$/ig, "");
            preload.push(spinnerSrc);
        }

        for (var i = 0, len = preload.length; i < len; ++i) {
            if (typeof(preload[i]) != 'string' || preload[i].toLowerCase() == 'none') {
                continue;
            }
            $('<img/>')[0].src = preload[i];
        }

        _data.pluginImagesPreloaded = true;
    }

    /**
     * Attaches load and error events to an array of images.
     *
     * @param {Object} images jQuery array of image elements
     * @private
     */
    function _bindLoad(images) {
        $(images).each(function () {
            // already loaded/cached
            if (this.complete) {
                _display(this);
                return;
            }


            $(this).one("load", function (e) {
                _display(this);
            })
                .one("error", _displayErrorImg);
        });
    }

    /**
     * Changes the src of an image that fails to load.  Conserves
     * original src value in attribute orig-src.
     *
     * @param {Object} e event object
     * @private
     * @return {Boolean}
     */
    function _displayErrorImg(e) {
        var $this = $(this),
            _data = _getData($this);

        if (this.src == _data.opt.imageNotFound) {
            return false
        }
        ;
        var origSrc = $(this).attr('src');
        $this.attr({'orig-src':origSrc,
            'src':_data.opt.imageNotFound});

        if ($this.attr('complete')) {
            _display(this);
            return true;
        }

        $(this).one("load", function (e) {
            _display(this);
        });
        return true;
    }

    /**
     * Centers images, and displays the first one in the image group.
     *
     * @param {Object} image object or array of image objects
     * @private
     */
    function _display(images) {
        var $images = $(images),
            _data = _getData($images);

        return $images.each(function () {
            var $this = $(this);

            $this.unbind('load');

            // center it
            _center(this);

            // hide any loading indicator
            _hideNextLoader($this.closest('.' + _data.opt.wrapClass));

            // if the image has no siblings, and is wrapped with an anchor,
            // determine if it's the first in the group
            if (!$this.siblings().length && $this.parent('a').length) {
                if (!$this.parent('a').prev().length) {
                    $this.css('display', 'inline');
                    _hideBgLoader(this);
                }
                return;
            }

            // if it's the first image in the group, display it
            if (!$this.prev().length) {
                $this.css('display', 'inline');
                _hideBgLoader(this);
            }
        });
    }

    /**
     * Displays an animated loading indicator when the
     * next image in the rotation has not yet been loaded
     * by the browser.
     *
     * @param {Object} imageWrap image-wrap element
     * @private
     */
    function _showNextLoader(imageWrap) {
        $(imageWrap).children('.overlay')
            .children('.rotate-state-button')
            .addClass('loading');
    }

    /**
     * Hides the animated loading indicator.
     *
     * @param {Object} imageWrap image-wrap element
     * @private
     */
    function _hideNextLoader(imageWrap) {
        $(imageWrap).children('.overlay')
            .children('.rotate-state-button')
            .removeClass('loading');
    }

    /**
     * Removes the background loading indicator displayed
     * in the image wrap
     *
     * @param {Object} e event object or image-wrap element
     * @private
     */
    function _hideBgLoader(el) {
        var $el = $(el),
            _data = _getData($el);

        if (!$el.hasClass(_data.opt.wrapClass)) {
            $el.closest('.' + _data.opt.wrapClass).removeClass('loading');
            return;
        }
        $el.removeClass('loading');
    }

    /**
     * Adds the image-wrap to an image group, and "crops"
     * the displayed image. Also adds the overlay and play/pause button.
     *
     * @param {Object} el The parent li container
     * @private
     */
    function _crop(el) {
        var $el = $(el),
            _data = _getData($el);

        $el.css({
            "width":_data.opt.crop[0],
            "height":_data.opt.crop[1]
        });

        // wrap the images only if the imgWrap isn't there yet
        if ($el.children('.' + _data.opt.wrapClass).length) {
            return;
        }

        var $imgWrap = $("<div></div>"),
            imgWrapClass = _data.opt.wrapClass,
            $imgs = $el.find('img').not(_data.opt.excludeSelector),
            $anchors = $el.find('a').has('img').not(_data.opt.excludeSelector);   // anchors containing images

        if (_data.opt.roundCorners) {
            imgWrapClass += ' round';
        }
        if (_data.opt.showLoader) {
            imgWrapClass = imgWrapClass + ' loading';
        }

        $imgWrap.attr({"class":imgWrapClass});

        // wrap around the $anchors
        if ($anchors.length) {
            $anchors.wrapAll($imgWrap);
        }
        else {
            // wrap around the images
            $imgs.wrapAll($imgWrap);
        }


        var countText = $imgs.not('[thumb]').length ? '1/' + $imgs.not('[thumb]').length : '',
            $overlayTop = $('<div class="overlay top"><a class="back" href="">&#9664&nbsp;&nbsp;Back</a>' +
                '<div class="count">' + countText + '</div>' +
                '</div>').css('display', 'none'),
            $overlayBottom = $('<div class="overlay bottom"></div>').css('display', 'none'),
            $spanText = $('<span></span>');

        $overlayBottom.append($spanText);

        if (_data.opt.playPauseButton) {
            var button = $('<a title="pause"></a>').addClass('rotate-state-button');
            $overlayBottom.append(button);
        }

        $overlayBottom.append($spanText);

        $imgWrap = $el.children('.' + _data.opt.wrapClass);
        $imgWrap.append($overlayTop, $overlayBottom);

        // bind rotation play/pause event
        if (_data.opt.playPauseButton && !_data.touch.enabled) {
            $imgWrap.children('.overlay.bottom')
                .children('.rotate-state-button')
                .bind('click' + _data.opt.bindNamespace, _playPause);
        }
    }

    /**
     * Event handler for play/pause button
     *
     * @this {Object} Target element
     * @param e $e Event object
     * @private
     * @return false
     */
    function _playPause(e) {
        if (e != undefined) {
            e.preventDefault();
        }

        var _data = _getData(e.target);

        // currently loading next image, ignore event
        if (_data.opt.showLoader && $(this).hasClass('loading')) {
            return false;
        }

        // play -> pause, pause -> play
        _flipRotateState($(this).parents('.' + _data.opt.wrapClass), this);
        return false;
    }

    /**
     * Centers an image(s) within it's crop container. Assumes
     * that the image(s) have been loaded and their actual height/width
     * is available.
     *
     * @param {Object} image object or array of images
     * @private
     */
    function _center(img) {
        // assuming image has been loaded already
        return $(img).each(function () {
            var $this = $(this),
                width = $this.width(),
                height = $this.height();

            $this.css({
                'margin-top':'-' + ( parseInt(height) / 2 ) + 'px',
                'margin-left':'-' + ( parseInt(width) / 2 ) + 'px',
                'top':'50%',
                'left':'50%'
            });

        });
    }

    /**
     * Removes the centering for an image(s)
     *
     * @this {Object} jQuery array of images
     * @private
     */
    function _uncenter() {
        return this.each(
            function () {
                $(this).css({
                    'margin-top':0,
                    'margin-left':0,
                    'left':0,
                    'top':0
                });
            }
        )
    }

    /**
     * Adjusts the size of an expansion based on the
     * dimensions of an image to be displayed.
     *
     * @param {Object} imgWrap image-wrap element
     * @param {Object} img
     * @param {Function} cb callback
     * @private
     */
    function _expandResize(imgWrap, img, cb) {
        var $imgWrap = $(imgWrap),
            _data = _getData($imgWrap),
            $img = $(img),
            imgW = $img.width(),
            imgH = $img.height(),
            imageWrapW = $imgWrap.width(),
            imageWrapH = $imgWrap.height(),
            __done = typeof cb === 'function' ? cb : undefined;

        // don't shrink wrapper to image 
        if (_data.opt.retainMaxExpansion && maxDim && (maxDim[0] <= imgWrapW) && (maxDim[1] <= imgWrapH)) {
            if (__done !== undefined) {
                __done();
            }
            return;
        }

        // resize wrapper div, reposition image
        var mleft = imgW,
            mtop = imgH,
            expandArgs = {
                'margin-top':0,
                'margin-left':0,
                'top':'50%',
                'left':'50%',
                'width':imgW,
                'height':imgH
            };

        // retain the dimension of the largest expanded $imgWrap
        if (_data.opt.retainMaxExpansion) {
            var maxDim = $imgWrap.data('maxDim'),
                setMaxDim = false;

            // maxDim not yet set for this group
            if (maxDim == undefined || maxDim.length != 2) {
                setMaxDim = true;
            }
            else {
                // set the $imgWrap to be the maxDim width 
                if (maxDim[0] > expandArgs.width) {
                    expandArgs.width = maxDim[0];
                    mleft = maxDim[0];
                }
                else {
                    setMaxDim = true;
                }

                // set the $imgWrap to be the maxDim height
                if (maxDim[1] > expandArgs.height) {
                    expandArgs.height = maxDim[1];
                    mtop = maxDim[1];
                }
                else {
                    setMaxDim = true;
                }
            }

            if (setMaxDim) {
                _setGroupMaxDim($imgWrap, expandArgs.width, expandArgs.height);
            }

        }


        // check allowed expansion limits
        var expandDim = _applyExpansionLimits(expandArgs.width, expandArgs.height, _data.opt);
        expandArgs.width = expandDim[0];
        mleft = expandDim[0];
        expandArgs.height = expandDim[1];
        mtop = expandDim[1];

        // apply actual left/top values
        expandArgs['margin-left'] = '-' + ( parseInt(mleft) / 2 ) + 'px';
        expandArgs['margin-top'] = '-' + ( parseInt(mtop) / 2 ) + 'px';

        $imgWrap.stop(true, true);

        // no animation
        if (!_data.opt.expandSpeed) {
            $imgWrap.css(expandArgs);
            if (__done !== undefined) {
                __done();
            }
            return;
        }

        // animate
        $imgWrap.animate(
            expandArgs,
            {
                duration:_data.opt.expandSpeed,
                easing:_data.opt.expandEasing,
                complete:function () {
                    if (__done !== undefined) {
                        __done();
                    }
                }
            }
        );
    }

    /**
     * Sets the expansion dimensions in accordance with
     * the passed options' max expansion limits.
     *
     * @param {Number} width
     * @param {Number} height
     * @param {object} options object
     * @private
     * @return {Object} x,y array of updated dimensions
     */
    function _applyExpansionLimits(width, height, opt) {
        // min/max width
        if (width < opt.minExpansion[0]) {
            width = opt.minExpansion[0];
        }
        else if (width > opt.maxExpansion[0]) {
            width = opt.maxExpansion[0];
        }

        // min/max height
        if (height < opt.minExpansion[1]) {
            height = opt.minExpansion[1];
        }
        else if (height > opt.maxExpansion[1]) {
            height = opt.maxExpansion[1];
        }

        return [width, height];
    }

    /**
     * Touch mode only: called when an already expanded
     * image wrap is touched again
     *
     */
    function _touchExpanded($imgWrap, e) {
        if (e != undefined) {
            e.preventDefault();
        }

        var $target = $(e.target),
            $overlayTop = $imgWrap.children('.overlay.top');

        switch (true) {
            // play/pause
            case ($target.hasClass('rotate-state-button')):
                _playPause.call($target, e);

                var state = _getRotateState($imgWrap),
                    oarg = {
                        imgWrap:$imgWrap,
                        showTop:true,
                        fadeOutTopAfter:0
                    };

                // on replay, show overlay and fade it out
                // after the default seconds
                if (state == 'play') {
                    delete(oarg.fadeOutTopAfter);
                }

                _showOverlay(oarg);
                return;

            // back button
            case ($target.hasClass('back')):
                return _unrotate($imgWrap);

            // top overlay shown, and the image was clicked, then
            // trigger links/clicks/etc for parent anchor
            case ($overlayTop.is(':visible') &&
                $target.attr('nodeName').toLowerCase() == 'img'):
                var $a = $target.parent('a');
                if ($a.length) {

                    // eval onclick attr 
                    var onclick = $a.data('onclickEvent'),
                        href = $a.attr('href'),
                        target = $a.attr('target');

                    if (onclick) {
                        // trigger onclick
                        eval("(function(){" + onclick + "}); onclick();");
                    }

                    // trigger bound click events
                    var clickEvents = $a.data('clickEvents');
                    if (clickEvents != undefined && clickEvents.length) {
                        for (var i = 0; i < clickEvents.length; $i++) {
                            $a.trigger(clickEvents[i]);
                        }
                    }

                    // open link... 
                    if (href && !href.match(/^\s*javascript:/i)) {
                        // android browser doesn't support new windows via js?
                        if (target && target.toLowerCase() == '_blank') {
                            window.open(href, '_self');
                        }
                        else {
                            window.location.replace(href);
                        }
                        _unrotate($imgWrap);
                    }
                }
                return;
        }

        // if currently paused,
        // just return as the overlay should
        // already be showing
        if (_getRotateState($imgWrap) == 'pause') {
            return;
        }

        _showOverlay({
            imgWrap:$imgWrap,
            showTop:true
        });
    }

    /**
     * Expands image group.
     *
     * @this {element} image wrap element
     * @public
     */
    function _touchExpand() {
        var $this = $(this),
            _data = _getData($this),
            $imgWrap = $this.hasClass(_data.opt.wrapClass) ? $this : $this.find('.' + _data.opt.wrapClass);

        // return if expanded, touchstart event on body should
        // receive this event anyways
        if ($imgWrap.data('expanded')) {
            return;
        }

        // save it for later
        _data.touch.$expanded = $imgWrap;
        _saveData($imgWrap, _data);

        var dii = 0,
            $thumb = $imgWrap.find('img[thumb]').first();


        // if this is thumb, display the next image (if there is one)
        if ($thumb.length) {
            var $img = $imgWrap.find('img').not('[thumb]').first();
            if ($img.length) {
                $thumb.hide();
                dii++;
                $img.show().css({opacity:1.0});
            }
        }

        $imgWrap.data({
            expanded:true,
            displayedImageIndex:dii,
            humanStep:1
        }).parent('li').addClass('expanded');

        // hide url bar for ios devices
        //window.scrollTo(0,1);

        // hide any window scrollbars
        $("body").css("overflow", "hidden");

        // prevent scrollbars, trigger subsequent touches
        try {
            $('body').bind('touchstart' + _data.opt.bindNamespace, function (e) {
                return _touchExpanded.call(this, $imgWrap, e);
            });
        }
        catch (err) {
        }

        _showOverlay({
            imgWrap:$imgWrap,
            showTop:true
        });

        _touchPositionExpanded.call({imgWrap:$imgWrap}, {}, _data);

        $(window)
            // reposition on orientation change
            .bind('orientationchange' + _data.opt.bindNamespace, function (e) {
                _data.touch.orientationTriggered = true;
                _saveData($imgWrap, _data);
                _touchPositionExpanded({fade:false}, _data);

                // just in case scroll isn't triggered for some reason
                setTimeout(function () {
                    _data.touch.orientationTriggered = false;
                    _saveData($imgWrap, _data);
                }, 500);
            })
            // close expanded image on auto-scroll to top
            // (ie. status bar touched)
            .bind('scroll' + _data.opt.bindNamespace, function (e) {
                // make sure this scroll wasn't due to an
                // orientationchange
                if (_data.touch.orientationTriggered) {
                    _data.touch.orientationTriggered = false;
                    _saveData($imgWrap, _data);
                    return;
                }

                // ignore non-ios device
                if (!_data.touch.ios) {
                    return;
                }

                if ($(window).scrollTop() == 0) {
                    return _unrotate($imgWrap);
                }
            });
    }

    /**
     * (Re)positions the expanded image wrap and image
     *
     * @param {Object} arg.imgWrap The image wrap div jQuery object
     * @param {Boolean} arg.fade Fade in the image wrap
     * @param {Function} arg.callback Function called at end of positioning
     * @param {object} data object
     * @private
     */
    function _touchPositionExpanded(arg, _data) {
        arg = $.extend({}, {
                imgWrap:_data.touch.$expanded,
                fade:true,
                callback:undefined
            },
            arg);

        // get viewport and image dimensions
        var dii = arg.imgWrap.data('displayedImageIndex') ? arg.imgWrap.data('displayedImageIndex') : 0,
            $allImg = $(arg.imgWrap.find('img')),
            $img = $($allImg[dii]);


        var vw = window.innerWidth,
            vh = window.innerHeight,
            iOrigDim = $img.data('origDim'),
            iw = iOrigDim == undefined ? $img.width() : iOrigDim[0],
            ih = iOrigDim == undefined ? $img.height() : iOrigDim[1],
            expandTop = 0,
            expandLeft = 0;

        // save the img's original dimensions
        if (iOrigDim == undefined) {
            $img.data('origDim', [iw, ih]);
        }

        // resize image if necessary
        if (vw < iw) {
            iw = vw;
            ih = 'auto';
        }
        else if (vh < ih) {
            ih = vh;
            iw = 'auto';
        }


        // mobile safari version < 5 doesn't handle fixed positioning well,
        // so position expanded div to current scrolled position.
        if (_data.touch.ios && (typeof(_data.touch.iosv) == 'undefined' || _data.touch.iosv < 5)) {
            expandTop = window.pageYOffset;
            expandLeft = window.pageXOffset;
        }

        // enlarge the image wrap element to snap to screen edge
        var cssArgs = {
            position:'fixed',
            width:vw,
            height:vh,
            'top':expandTop,
            left:expandLeft,
        };

        // prepare a slight fade in
        if (arg.fade) {
            cssArgs.opacity = .4;
        }

        arg.imgWrap.css(cssArgs);

        // do a slight fade in 
        if (arg.fade) {
            arg.imgWrap.animate({opacity:1}, 'fast');
        }

        // expand it
        $img.css({
            width:iw,
            height:ih,
            position:'absolute'
        });

        // get computed width/height
        iw = $img.width();
        ih = $img.height();

        $img.css({
            'margin-left':'-' + ( (parseInt(iw) / 2) ) + 'px',
            'margin-top':'-' + ( (parseInt(ih) / 2) ) + 'px',
            'top':'50%',
            'left':'50%'
        });

        if (typeof(arg.callback) == 'function') {
            arg.callback();
        }
    }

    /**
     * Touch mode: unbinds touch events on image un-expansion
     *
     * @this {element} image wrap element
     * @param {object} data object
     * @private
     */
    function _touchShrinkUnbind(_data) {
        // reposition on orientation change,
        // unbind shrinkage on scroll
        $(window).unbind('orientationchange' + _data.opt.bindNamespace)
            .unbind('scroll' + _data.opt.bindNamespace);

        // restore any window scrollbars
        // resume scrolling
        $('body').css("overflow", "auto")
            .unbind('touchstart' + _data.opt.bindNamespace);

        // update overlay count
        if ($(this).hasClass(_data.opt.wrapClass)) {
            _touchUpdateOverlayCount({
                    imgWrap:this,
                    current:1
                },
                _data);

            $(this).data('humanStep', 1);
        }
    }

    /**
     * Plugin definition
     *
     * @param {String} method Method name
     * @public
     */
    jQuery.fn.peepshow = function (method) {
        // Method calling logic
        if (methods[method]) {
            return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
        }
        else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        }
        else {
            $.error('Method ' + method + ' does not exist on jQuery.peepshow');
        }
    };

    /**
     * Default options.
     */
    jQuery.fn.peepshow.defaults = {
        autoCenter:true, // centers the entire image gallery in its container
        autoMargin:true, // automatically spaces out image groups
        autoMarginRatio:.25, // increase for larger auto margins, decrease for smaller
        bindNamespace:'.jq-peepshow', // namespace used for binding events
        captionAttribute:'caption', // img element attribute used to populate the image's overlay caption
        crop:[200, 200], // thumbnail imensions in pixels [x,y]
        excludeSelector:'img.exclude', // selector for images to ignore in the image gallery
        expandEasing:"swing", // easing for expansion animation
        expandSpeed:0, // expansion animation speed (milliseconds). Null/false for
        // no animation
        hoverIntentOptions:{                       // hoverItent options
            sensitivity:9,
            interval:100,
            timeout:0
        },
        imageNotFound:'img/image-not-found.jpg', // image not found src
        itemsPerRow:3, // image groups per row. "auto" for automatic items per row
        // determination. Set to false or 0 if you don't want
        // the plugin to automatically apply css to make rows.
        maxExpansion:[500, 500], // maximum expansion dimensions in pixels [x,y]
        minExpansion:[300, 300], // minimum expansion dimensions in pixels [x,y]
        playPauseButton:false, // display the pause/play button
        retainMaxExpansion:true, // set to false to always resize the image wrapper
        // to the actual size of the image.
        rotateSpeed:1700, // Speed at which images are rotated (milliseconds).
        roundCorners:false, // Round image corners
        showLoader:true, // Display animated image loading indicators.
        shrinkEasing:"swing", // easing for shrink animation. Null/false for no animation.
        shrinkSpeed:0, // animation speed (milliseconds)
        touchMode:'auto', // touch device mode. 'auto' for auto-detection, true/false to force.
        transitionEffect:'fade', // Rotation transition effects.
        transitionEffectSpeedRatio:.27, // Speed of the fade transition as a function of the
        // rotateSpeed option. Increase for a slower fade,
        // decrease for a faster fade.
        wrapClass:'image-wrap'    // image wrap css class
    };

})(jQuery);


/*****************/
/** hoverIntent **/
/*****************/

/*!
 * hoverIntent is similar to jQuery's built-in "hover" function except that
 * instead of firing the onMouseOver event immediately, hoverIntent checks
 * to see if the user's mouse has slowed down (beneath the sensitivity
 * threshold) before firing the onMouseOver event.
 *
 * hoverIntent r6 // 2011.02.26 // jQuery 1.5.1+
 * <http://cherne.net/brian/resources/jquery.hoverIntent.html>
 *
 * hoverIntent is currently available for use in all personal or commercial
 * projects under both MIT and GPL licenses. This means that you can choose
 * the license that best suits your project, and use it accordingly.
 *
 * // basic usage (just like .hover) receives onMouseOver and onMouseOut functions
 * $("ul li").hoverIntent( showNav , hideNav );
 *
 * // advanced usage receives configuration object only
 * $("ul li").hoverIntent({
 *   sensitivity: 7, // number = sensitivity threshold (must be 1 or higher)
 *   interval: 100,   // number = milliseconds of polling interval
 *   over: showNav,  // function = onMouseOver callback (required)
 *   timeout: 0,   // number = milliseconds delay before onMouseOut function call
 *   out: hideNav    // function = onMouseOut callback (required)
 * });
 *
 * @param  f  onMouseOver function || An object with configuration options
 * @param  g  onMouseOut function  || Nothing (use configuration options object)
 * @author    Brian Cherne brian(at)cherne(dot)net
 * @modifiedBy  Timbo White, ZULIUS LLC
 *
 *
 */
(function ($) {
    $.fn.hoverIntent = function (f, g) {
        // default configuration options
        var cfg = {
            sensitivity:7,
            interval:100,
            timeout:0,
            bindNamespace:'.hoverIntent'
        };

        // override configuration options with user supplied object
        cfg = $.extend(cfg, g ? { over:f, out:g } : f);

        // instantiate variables
        // cX, cY = current X and Y position of mouse, updated by mousemove event
        // pX, pY = previous X and Y position of mouse, set by mouseover and polling interval
        var cX, cY, pX, pY;

        // A private function for getting mouse position
        var track = function (ev) {
            cX = ev.pageX;
            cY = ev.pageY;
        };

        // A private function for comparing current and previous mouse position
        var compare = function (ev, ob) {
            ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
            // compare mouse positions to see if they've crossed the threshold
            if (( Math.abs(pX - cX) + Math.abs(pY - cY) ) < cfg.sensitivity) {
                $(ob).unbind("mousemove" + cfg.bindNamespace, track);
                // set hoverIntent state to true (so mouseOut can be called)
                ob.hoverIntent_s = 1;
                return cfg.over.apply(ob, [ev]);
            } else {
                // set previous coordinates for next time
                pX = cX;
                pY = cY;
                // use self-calling timeout, guarantees intervals are spaced out properly (avoids JavaScript timer bugs)
                ob.hoverIntent_t = setTimeout(function () {
                    compare(ev, ob);
                }, cfg.interval);
            }
        };

        // A private function for delaying the mouseOut function
        var delay = function (ev, ob) {
            ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
            ob.hoverIntent_s = 0;
            return cfg.out.apply(ob, [ev]);
        };

        // A private function for handling mouse 'hovering'
        var handleHover = function (e) {
            // copy objects to be passed into t (required for event object to be passed in IE)
            var ev = jQuery.extend({}, e);
            var ob = this;

            // cancel hoverIntent timer if it exists
            if (ob.hoverIntent_t) {
                ob.hoverIntent_t = clearTimeout(ob.hoverIntent_t);
            }

            // if e.type == "mouseenter"
            if (e.type == "mouseenter") {
                // set "previous" X and Y position based on initial entry point
                pX = ev.pageX;
                pY = ev.pageY;
                // update "current" X and Y position based on mousemove
                $(ob).bind("mousemove" + cfg.bindNamespace, track);
                // start polling interval (self-calling timeout) to compare mouse coordinates over time
                if (ob.hoverIntent_s != 1) {
                    ob.hoverIntent_t = setTimeout(function () {
                        compare(ev, ob);
                    }, cfg.interval);
                }

                // else e.type == "mouseleave"
            } else {
                // unbind expensive mousemove event
                $(ob).unbind("mousemove" + cfg.bindNamespace, track);
                // if hoverIntent state is true, then call the mouseOut function after the specified delay
                if (ob.hoverIntent_s == 1) {
                    ob.hoverIntent_t = setTimeout(function () {
                        delay(ev, ob);
                    }, cfg.timeout);
                }
            }
        };

        // bind the function to the two event listeners
        return this.bind('mouseenter' + cfg.bindNamespace, handleHover).bind('mouseleave' + cfg.bindNamespace, handleHover);
    };
})(jQuery);
