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
  getcode("js/mod/redgenes/workers.js", "workers", withbase);
}

function withbase(c)
{
  c.source = "self.importScripts('"+self.cljs.url+"');/n"+c.source;
  c.code = new Blob([c.source], {type: 'text/javascript'});
  c.url = URL.createObjectURL(c.code);
  self.codez[c.name] = c;
}

getcode("js/mod/cljs_base.js", "cljs", setcode);

//# sourceMappingURL=main.js.map