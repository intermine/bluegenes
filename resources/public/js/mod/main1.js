redgenes.main = {};

console.log("hi");

self.codez = {};

function getcode(file, name, g){
try
{
var oReq = new XMLHttpRequest();
oReq.addEventListener("load",
  function(e){console.log("resp");
    try{
    var code = new Blob([this.responseText], {type: 'text/javascript'});
    var url = URL.createObjectURL(code);
    g({url:  url,
      code: code,
      source: this.responseText,
      name: name});
    console.log("got ", name);
    }catch(ee){console.log(ee);}
    });
oReq.open("GET", file);
oReq.send();
console.log("getting ", name);
}
catch(e){console.log(e);}
}

function setcode(c)
{
  self.codez[c.name] = c;
}

function withbase(c, base)
{

}

getcode("js/mod/cljs_base.js", "cljs", setcode);
getcode("js/mod/redgenes/workers.js", "workers", setcode);

//# sourceMappingURL=main.js.map