$(function(){

    var options = {
        nodes: {
            shape: 'box',
            margin: 20
        },
        edges: {
            font: { align: 'middle' }
        },
        layout: {
            hierarchical: { direction: "LR" }
        },
        physics:false
    };

    $.when(
        $.get("/api/nodes"),
        $.get("/api/edges")
    ).done(function (ns, es) {
        var data = {
            nodes: ns[0].nodes.map(function(n){
                switch(n.kind) {
                    case 'commit':
                        n.color = { background: '#faa' };
                        return n;
                    case 'tree':
                        n.color = { background: '#afa' };
                        return n;
                    case 'file':
                        n.color = { background: '#aaf' };
                        return n;
                }
            }),
            edges: es[0].edges.map(function(e){
                  switch(e.kind) {
                      case 'commit':
                          return { from: e.from, to: e.to, arrows: 'to' };
                      case 'tree':
                          return { from: e.from, to: e.to, arrows: 'to' };
                      case 'file':
                          return { from: e.from, to: e.to, arrows: 'to', label: e.label };
                  }
              }),
        };

        var container = document.getElementById('mynetwork');
        var network = new vis.Network(container, data, options);
    });

});