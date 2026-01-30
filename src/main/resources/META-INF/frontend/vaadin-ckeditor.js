import {html, LitElement} from 'lit';
import {classMap} from 'lit/directives/class-map.js';
import './ckeditor';

export class VaadinCKEditor extends LitElement {

    constructor() {
        super();
        this.classes = {
            'editor-content'  : true
        };
        this.config = {};
        this.version = 'v5.0-SNAPSHOT'
        this.autosave = false;
        this.sync = true;
        this.position = 0;
        this.isFirefox = typeof InstallTrigger !== 'undefined';
        this.isChrome = !!window.chrome && (!!window.chrome.webstore || !!window.chrome.runtime);
    }

    // loadLanguage(scriptUrl) {
    //     let script = document.createElement('script');
    //     script.src = scriptUrl;
    //     document.head.appendChild(script);
    //     return new Promise((success, failed) => {
    //         script.onload = function() {
    //             success();
    //         }
    //         script.onerror = function () {
    //             failed();
    //         }
    //     });
    // }

    static get properties() {
        return { editorId: String,
                 label: String,
                 editorType: String,
                 editorData: String,
                 editorWidth: String,
                 editorHeight: String,
                 themeType: String,
                 errorMessage: String,
            	 overrideCssUrl: String,
                 text: String,
                 miniMapEnabled:Boolean,
                 isReadOnly: Boolean,
                 isFirefox: Boolean,
                 isChrome: Boolean,
                 autosave: Boolean,
                 waitingTime: Number,
                 sync: Boolean,
                 ghsEnabled: Boolean,
                 hideToolbar: Boolean,
                 editorMap: Object,
                 config: Object}
    }

    createRenderRoot() {
        return this
    }

    initDarkTheme() {
        let darkStyle = document.querySelector(':root').style;
        darkStyle.setProperty('--ck-color-base-foreground', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-base-background', 'hsl(0, 0%, 77%)');
        darkStyle.setProperty('--ck-color-focus-border', 'hsl(208, 90%, 62%)');
        darkStyle.setProperty('--ck-color-text', 'hsl(0, 0%, 98%)');
        darkStyle.setProperty('--ck-color-shadow-drop', 'hsla(0, 0%, 0%, 0.2)');
        darkStyle.setProperty('--ck-color-shadow-inner', 'hsla(0, 0%, 0%, 0.1)');
        darkStyle.setProperty('--ck-color-button-default-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-button-default-hover-background', 'hsl(270, 1%, 22%)');
        darkStyle.setProperty('--ck-color-button-default-active-background', 'hsl(270, 2%, 20%)');
        darkStyle.setProperty('--ck-color-button-default-active-shadow', 'hsl(270, 2%, 23%)');
        darkStyle.setProperty('--ck-color-button-default-disabled-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-button-on-background', 'hsl(255, 3%, 18%)');
        darkStyle.setProperty('--ck-color-button-on-hover-background', 'hsl(255, 4%, 16%)');
        darkStyle.setProperty('--ck-color-button-on-active-background', 'hsl(255, 4%, 14%)');
        darkStyle.setProperty('--ck-color-button-on-active-shadow', 'hsl(240, 3%, 19%)');
        darkStyle.setProperty('--ck-color-button-on-disabled-background', 'hsl(255, 3%, 18%)');
        darkStyle.setProperty('--ck-color-button-action-background', 'hsl(168, 76%, 42%)');
        darkStyle.setProperty('--ck-color-button-action-hover-background', 'hsl(168, 76%, 38%)');
        darkStyle.setProperty('--ck-color-button-action-active-background', 'hsl(168, 76%, 36%)');
        darkStyle.setProperty('--ck-color-button-action-active-shadow', 'hsl(168, 75%, 34%)');
        darkStyle.setProperty('--ck-color-button-action-disabled-background', 'hsl(168, 76%, 42%)');
        darkStyle.setProperty('--ck-color-button-action-text', 'hsl(0, 0%, 100%)');
        darkStyle.setProperty('--ck-color-button-save', 'hsl(120, 100%, 46%)');
        darkStyle.setProperty('--ck-color-button-cancel', 'hsl(15, 100%, 56%)');
        darkStyle.setProperty('--ck-color-dropdown-panel-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-dropdown-panel-border', 'hsl(255, 3%, 18%)');
        darkStyle.setProperty('--ck-color-split-button-hover-border', 'hsl(255, 3%, 18%)');
        darkStyle.setProperty('--ck-color-input-background', 'hsl(255, 3%, 18%)');
        darkStyle.setProperty('--ck-color-input-border', 'hsl(257, 3%, 43%)');
        darkStyle.setProperty('--ck-color-input-text', 'hsl(0, 0%, 98%)');
        darkStyle.setProperty('--ck-color-input-disabled-background', 'hsl(255, 4%, 21%)');
        darkStyle.setProperty('--ck-color-input-disabled-border', 'hsl(250, 3%, 38%)');
        darkStyle.setProperty('--ck-color-input-disabled-text', 'hsl(0, 0%, 46%)');
        darkStyle.setProperty('--ck-color-list-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-panel-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-panel-border', 'hsl(300, 1%, 22%)');
        darkStyle.setProperty('--ck-color-toolbar-background', 'hsl(270, 1%, 29%)');
        darkStyle.setProperty('--ck-color-toolbar-border', 'hsl(300, 1%, 22%)');
        darkStyle.setProperty('--ck-color-tooltip-background', 'hsl(252, 7%, 14%)');
        darkStyle.setProperty('--ck-color-tooltip-text', 'hsl(0, 0%, 93%)');
        darkStyle.setProperty('--ck-color-image-caption-background', 'hsl(0, 0%, 97%)');
        darkStyle.setProperty('--ck-color-image-caption-text', 'hsl(0, 0%, 20%)');
        darkStyle.setProperty('--ck-color-widget-blurred-border', 'hsl(0, 0%, 87%)');
        darkStyle.setProperty('--ck-color-widget-hover-border', 'hsl(43, 100%, 68%)');
        darkStyle.setProperty('--ck-color-widget-editable-focus-background', 'hsl(0, 0%, 100%)');
        darkStyle.setProperty('--ck-color-link-default', 'hsl(190, 100%, 75%)');
    }

    firstUpdated(changedProperties) {
        super.firstUpdated(changedProperties)

        if(this.themeType==='dark') {
            this.initDarkTheme()
        }

        this.createEditor()
    }

    getEditorByType(editorType) {
        if(window.CKEDITOR) {
            return 'classic'===editorType?window.CKEDITOR.VaadinClassicEditor:
                   'inline'===editorType?window.CKEDITOR.VaadinInlineEditor:
                   'balloon'===editorType?window.CKEDITOR.VaadinBalloonEditor:
                   'decoupled'===editorType?window.CKEDITOR.VaadinDcoupledEditor:
                       window.CKEDITOR.VaadinClassicEditor
        } else {
            throw new Error('CKEditor is not loaded!')
        }

    }

    getConfig() {//check if contains function
        let configuration = this.autosave===true? {
            ...this.config,
            ...{
                autosave: {
                    save( editor ) {
                        return window.vaadinCKEditor.saveData( editor.id, editor.getData() );
                    },
                    waitingTime: this.waitingTime?this.waitingTime:2000
                }
            }
        } : this.config;
        configuration = this.ghsEnabled===true? {
            ...configuration,
            ...{
                htmlSupport: {
                    allow: [
                        {
                            name: /.*/,
                            attributes: true,
                            classes: true,
                            styles: true
                        }
                    ]
                }
            }
        } : configuration;
        let minimap = {
                minimap: {
                    container: document.querySelector( '.minimap-container' )
            }
        };
        if(this.editorType==='decoupled') {
            return {
                ...minimap,
                ...configuration
            };
        } else {
            return configuration;
        }
    }


    createEditor() {
        this.getEditorByType(this.editorType).create(this.querySelector( "#"+this.editorId ) , this.getConfig()).then( editor => {
            editor.id = this.editorId;
            const toolbar = editor.ui.view.toolbar;
            window.vaadinCKEditor.serverMap[editor.id] = this.$server;
            if(this.isReadOnly) {
                editor.enableReadOnlyMode( this.editorId );
            } else {
                editor.disableReadOnlyMode(this.editorId);
            }
            if(toolbar) {
                if(this.hideToolbar) {
                    toolbar.element.style.display = 'none';
                } else {
                    toolbar.element.style.display = 'flex';
                }
            }
            console.log({...{"ckeditor-vaadin":this.version},
                         ...this.getConfig()});

            editor.setData(this.editorData?this.editorData:'');
            this.style.width = this.style.width ? this.style.width :
                               this.isChrome ? '-webkit-fill-available':
                               this.isFirefox ? '-moz-available': '100%';
            this.style.height='100%';
            if(this.required) {
                window.vaadinCKEditor.showIndicator(editor,true);
                window.vaadinCKEditor.showError(editor,this.invalid);
            }
            editor.editing.view.change( writer => {
                if(this.editorHeight) {
                    writer.setStyle( 'height', this.editorHeight, editor.editing.view.document.getRoot());
                }
            } );

            window.vaadinCKEditor.sourceEditObserver(editor, this.required, this.invalid);

            editor.ui.focusTracker.on( 'change:isFocused', ( evt, data, isFocused ) => {
                if(this.editorType==='classic') {
                    window.vaadinCKEditor.activateScroll(editor);
                }

                if(evt.source.focusedElement && typeof evt.source.focusedElement !== 'undefined') {
                    let sourceEdit = evt.source.focusedElement?.offsetParent;
                    if(sourceEdit && sourceEdit.className === 'ck-source-editing-area') {
                        window.vaadinCKEditor.sourceDataObserverMap[editor.id]?.observe(sourceEdit, {
                            attributes: true //configure it to listen to attribute changes
                        });
                    }
                } else {
                    let sourceEdit = document.querySelector("#"+editor.id).parentElement.querySelector("div.ck-source-editing-area");
                    if(sourceEdit) {//loose focus action in source edit mode
                        let sourceEditValue = sourceEdit.getAttribute("data-value");
                        window.vaadinCKEditor.setAndCheck(editor, sourceEditValue, this.required, this.invalid);
                    }
                }
            } );

            editor.editing.view.document.on( 'change:isFocused', ( evt, data, isFocused ) => {
                if(!this.sync) {
                    let editorData = editor.getData();
                    window.vaadinCKEditor.focusedColor(editor, isFocused);
                    window.vaadinCKEditor.serverMap[editor.id].setEditorData(editorData);
                    this.checkEditorData(editor);
                }
            } );

            editor.model.document.selection.on('change:range', (evt, data) => {
                const selection = editor.model.document.selection;
                this.position = selection.getFirstPosition();
            });

            editor.model.document.on( 'change:data', (event, batch) => {
                if( this.sync ) {
                    window.vaadinCKEditor.serverMap[editor.id].setEditorData(editor.getData());
                    this.checkEditorData(editor);
                }
            } );

            editor.on( 'change:isReadOnly', ( evt, propertyName, isReadOnly ) => {
                if ( isReadOnly ) {
                    editor.enableReadOnlyMode( editor.id );
                } else {
                    editor.disableReadOnlyMode( editor.id );
                }
                if(toolbar) {
                    if ( this.hideToolbar ) {
                        toolbar.element.style.display = 'none';
                    } else {
                        toolbar.element.style.display = 'flex';
                    }
                }
            } );
            window.vaadinCKEditor.saveData = function( editorId, editorData ) {
                return new Promise( resolve => {
                    setTimeout( () => {
                        window.vaadinCKEditor.serverMap[editorId].saveEditorData(editorData);
                        resolve();
                    }, 400 );
                } );
            }
            window.vaadinCKEditor.editorMap[editor.id] = editor;
            if(this.editorType==='decoupled') {
                document.querySelector( '#toolbar-container' ).appendChild( editor.ui.view.toolbar.element );
                // document.querySelector( "#"+this.editorId ).appendChild( editor.ui.view.editable.element );
                editor.ui.update();
            }

            if(this.overrideCssUrl) {
                window.vaadinCKEditor.importStyle(this, this.overrideCssUrl);
            }
        } ).catch( err => {
            console.error( err.stack );
        } );
    }

    checkEditorData(editor){
        let indicated = window.vaadinCKEditor.empty.includes(editor.getData()) && this.required;
        window.vaadinCKEditor.showIndicator(editor, indicated);
        window.vaadinCKEditor.showError(editor,this.invalid || indicated);
    };

    updateData(editorId, value) {
        if(window.vaadinCKEditor.editorMap && window.vaadinCKEditor.editorMap[editorId]) {
            window.vaadinCKEditor.editorMap[editorId].setData(value);
        }
    }

    insertText(editorId, text) {
        if(window.vaadinCKEditor.editorMap && window.vaadinCKEditor.editorMap[editorId]) {
            let editor = window.vaadinCKEditor.editorMap[editorId];
            editor.model.change(writer => {
                writer.insertText(text, this.position);
                this.checkEditorData(editor);
            });
        }
    }

    fire(editorId, event, property, value) {
        if(window.vaadinCKEditor.editorMap && window.vaadinCKEditor.editorMap[editorId]) {
            window.vaadinCKEditor.editorMap[editorId].fire(event, property, value);
        }
    }

    render() {
        return html`
            <label part="label" id="label_${this.editorId}">${this.label} </label>
            <ul part="label-ul"><li part="label-li">
                <div part="error-message" id="error_${this.editorId}">${this.errorMessage}</div>
            </li></ul>
            
            ${this.editorType==='decoupled' ? html`
                <div id="document-container">
                    <div id="toolbar-container">
                        <!-- This is where the document editor toolbar will be inserted. -->
                    </div>
                    <div class="minimap-wrapper">
                        <div class="editor-container">
                            <div id="${this.editorId}" class=${classMap(this.classes)}>
                                <!-- This is where the edited content will render (the page). -->
                            </div>
                        </div>
                        <div class="minimap-container" style="display: ${ this.miniMapEnabled ? 'block' : 'none' }">
                            <!-- This is where the minimap will be inserted. -->
                        </div>
                    </div>
                </div>
            `:html`<div id="${this.editorId}"/>`}
        `;
    }

    validate() {
        return !(this.focusElement.invalid = this.invalid);
    };

}

customElements.get('vaadin-ckeditor') || customElements.define('vaadin-ckeditor', VaadinCKEditor);
