
//A workaround for charset encoding problems
%typemap(jni) jbyte[]  "jbyteArray"
%typemap(jtype) jbyte[] "byte[]"
%typemap(jstype) jbyte[] "byte[]"
%typemap(in) jbyte[]
{
	int length = JCALL1(GetArrayLength, jenv, $input);
	jbyte * name = new jbyte[length+1];
	JCALL4(GetByteArrayRegion, jenv, $input, 0, length, name); 
	name[length] = '\0';
	$1 = name;
}
%typemap(javain) jbyte[] "$javainput"


/**
 * CAFControl_Reader
 */
 %{
#include <XCAFApp_Application.hxx>
#include <XCAFDoc_DocumentTool.hxx>
#include <IGESCAFControl_Reader.hxx>
#include <STEPCAFControl_Reader.hxx>
#include <TDocStd_Document.hxx>

#include <IGESControl_Reader.hxx>
#include <STEPControl_Reader.hxx>

#include <XSControl_WorkSession.hxx>
#include <XSControl_TransferReader.hxx>
#include <StepRepr_RepresentationItem.hxx>
#include <TCollection_HAsciiString.hxx>
#include <IGESData_IGESEntity.hxx>
#include <TransferBRep.hxx>
#include <Transfer_Binder.hxx>
#include <Transfer_TransientProcess.hxx>
#include <Interface_InterfaceModel.hxx>

#include <Quantity_Color.hxx>
#include <XCAFDoc_ColorTool.hxx>
#include <XCAFDoc_ShapeTool.hxx>
#include <TDF_LabelSequence.hxx>

#include <iostream>
%}
 

%rename(TDocStd_Document) Handle_TDocStd_Document;

class Handle_TDocStd_Document
{
    public:
	Handle_TDocStd_Document();
	
	
};
%extend Handle_TDocStd_Document
{
    TDF_Label Main() const{
       return (*self)->Main(); 
    }
};

class XCAFApp_Application
{
	XCAFApp_Application()=0;
    public:
};
%extend XCAFApp_Application
{
	static XCAFApp_Application * GetApplication()
	{
		Handle_XCAFApp_Application hgc = XCAFApp_Application::GetApplication();
		if(hgc.IsNull())
			return NULL;
		else
			return (XCAFApp_Application*)(Standard_Transient*)hgc;
	}
	void NewDocument(const Standard_CString filename,Handle_TDocStd_Document& aDoc) {
	    self->NewDocument(filename, aDoc);
	}
};

%rename(XCAFDoc_ShapeTool) Handle_XCAFDoc_ShapeTool;
%rename(XCAFDoc_ColorTool) Handle_XCAFDoc_ColorTool;
%rename(XCAFDoc_LayerTool) Handle_XCAFDoc_LayerTool;
%rename(XCAFDoc_DimTolTool) Handle_XCAFDoc_DimTolTool;
%rename(XCAFDoc_MaterialTool) Handle_XCAFDoc_MaterialTool;

enum XCAFDoc_ColorType { 
    XCAFDoc_ColorGen,
    XCAFDoc_ColorSurf,
    XCAFDoc_ColorCurv
};

class XCAFDoc_ColorTool 
{
    %rename(getColor) GetColor;
    public:
    Standard_Boolean GetColor(const TopoDS_Shape& S,const XCAFDoc_ColorType ptype,Quantity_Color& color);   
};
/* TODO - This is more user friendly!
%extend XCAFDoc_ColorTool {
    Quantity_Color * getColor(const TopoDS_Shape& S,const XCAFDoc_ColorType ptype,Quantity_Color& color) {
        Quantity_Color color;
        if(GetColor(S, ptype, color))
            return new Quantity_Color(color);
        else
            return NULL; 
    }  
};*/

class XCAFDoc_DocumentTool
{
    //static Handle_XCAFDoc_ShapeTool ShapeTool(const TDF_Label& acces);
    //static Handle_XCAFDoc_ColorTool ColorTool(const TDF_Label& acces);
    //static Handle_XCAFDoc_LayerTool LayerTool(const TDF_Label& acces);
    //static Handle_XCAFDoc_DimTolTool DimTolTool(const TDF_Label& acces);
    //static Handle_XCAFDoc_MaterialTool MaterialTool(const TDF_Label& acces);
};
%extend XCAFDoc_DocumentTool{
    static XCAFDoc_ColorTool * ColorTool(const TDF_Label& acces) {
        Handle_XCAFDoc_ColorTool h = XCAFDoc_DocumentTool::ColorTool(acces);
		if(h.IsNull())
			return NULL;
		else
			return (XCAFDoc_ColorTool*)(Standard_Transient*)h;
    }
};

%typedef Standard_Real Quantity_Parameter;

class Quantity_Color
{
    %rename(red) Red;
    %rename(green) Green;
    %rename(blue) Blue;

    public:
    Quantity_Parameter Red() const;
    Quantity_Parameter Green() const;
    Quantity_Parameter Blue() const;
};
%extend Quantity_Color
{
    char* name()
    {
        return self->StringName ( self->Name() );
    }
    
};

class STEPCAFControl_Reader
{
	%javamethodmodifiers ReadFile(const Standard_CString filename)"
	/**
	 * @deprecated May segfault if path name include none-ASCII caracters. Use
	 * readFile(stringPath.getBytes()) instead.
	 */
	public";

    %rename(reader) Reader;
	%rename(readFile) ReadFile;
	%rename(nbRootsForTransfer) NbRootsForTransfer;
    %rename(setColorMode) SetColorMode;
    %rename(setNameMode) setNameMode;
    %rename(setLayerMode) SetLayerMode;
     
	public:
	STEPCAFControl_Reader();
	
	STEPControl_Reader& Reader();
	
	IFSelect_ReturnStatus ReadFile(const Standard_CString filename);
	Standard_Integer NbRootsForTransfer();
	
	void SetColorMode (const Standard_Boolean colormode);
	Standard_Boolean GetColorMode() const;
	void SetNameMode (const Standard_Boolean namemode);
	Standard_Boolean GetNameMode() const;
	void SetLayerMode (const Standard_Boolean layermode);
	Standard_Boolean GetLayerMode() const;
	
	Standard_Boolean Transfer(Handle_TDocStd_Document& doc);
};

%extend STEPCAFControl_Reader
{
	//A workaround for charset encoding problems
	IFSelect_ReturnStatus readFile(jbyte filename[])
	{
		return self->ReadFile((char*)filename);
	}
	
	TopoDS_Shape * oneShape(Handle_TDocStd_Document& doc) {
		TopoDS_Shape * ret;
	    TDF_LabelSequence Labels;
        Handle(XCAFDoc_ShapeTool) STool = XCAFDoc_DocumentTool::ShapeTool(doc->Main());
        STool->GetFreeShapes(Labels);
        if ( Labels.Length() <=0 ) {
            //di << "Document " << argv[2] << " contain no shapes" << "\n";
            ret = NULL;
        }

        if ( Labels.Length() ==1 ) {
            ret = new TopoDS_Shape();
            STool->GetShape ( Labels.Value(1), *ret );
        } else {
            TopoDS_Compound * C = new TopoDS_Compound();
            BRep_Builder B;
            B.MakeCompound ( *C );
            for ( Standard_Integer i = 1; i<= Labels.Length(); i++) {
              TopoDS_Shape S = STool->GetShape ( Labels.Value(i) );
              B.Add ( *C, S );
            }
            ret = C;
        }
        
        return ret;
	}
	
	
};



