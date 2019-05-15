import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame implements ActionListener {
    JPanel contentPane;
    JLabel jLabel1 = new JLabel();
    JTextField jTextField1 = new JTextField("127.0.0.1");
    JLabel jLabel2 = new JLabel();
    JTextField jTextField2 = new JTextField("4700");
    JButton jButton1 = new JButton();//“连接”按钮
    JLabel jLabel3 = new JLabel();
    JTextField jTextField3 = new JTextField();
    JButton jButton2 = new JButton();//“发送”按钮
    JButton jButton3 = new JButton();//“悔棋”按钮
    JScrollPane jScrollPane1 = new JScrollPane();
    JTextArea jTextArea1 = new JTextArea();
    BufferedReader instr = null;
    Socket socket =null;
    PrintWriter os = null;
    public static String[] ss = new String[10];
    //保存棋子的坐标
    int x = 0;
    int y = 0;
    int[][] allChess = new int[19][19];//保存之前下过的全部棋子坐标
    boolean isBlack = false;//自己是白棋方
    //标识当前游戏是否可以继续
    boolean canPlay = false;
    //保存显示的提示信息
    String message = "";//"自己是白棋，黑方先行";
    JPanel panel1 = new JPanel();
    GobangPanel panel2 = new GobangPanel();

    /**
     * 客户端构造方法
     */
    public Client(){
        jbInit();
    }

    private void jbInit(){
        contentPane = (JPanel) this.getContentPane();
        jLabel1.setFont(new Font("宋体",0,14));
        jLabel1.setText("服务器名称");
        jLabel1.setBounds(new Rectangle(20,22,87,28));

        //contentPane.setLayout(null);
        this.setSize(new Dimension(540,640));
        this.setTitle("客户端");

        jTextField1.setBounds(new Rectangle(114,26,108,24));
        jLabel2.setText("端口号");
        jLabel2.setFont(new Font("宋体",0,14));

        jButton1.setBounds(new Rectangle(400,28,73,25));
        jButton1.setFont(new Font("Dialog",0,14));
        jButton1.setBorder(BorderFactory.createEtchedBorder());
        jButton1.setActionCommand("jButton1");
        jButton1.setText("连接");

        jLabel3.setBounds(new Rectangle(23,57,87,28));
        jLabel3.setText("请输入信息");
        jLabel3.setFont(new Font("宋体",0,14));
        jTextField3.setBounds(new Rectangle(114,60,314,24));

        jButton2.setText("发送");
        jButton2.setActionCommand("jButton1");
        jButton2.setBorder(BorderFactory.createEtchedBorder());
        jButton2.setFont(new Font("Dialog",0,14));
        jButton2.setBounds(new Rectangle(440,58,73,25));

        jButton3.setText("悔棋");
        jButton3.setActionCommand("jButton1");
        jButton3.setBorder(BorderFactory.createEtchedBorder());
        jButton3.setFont(new Font("Dialog",0,14));
        jButton3.setBounds(new Rectangle(480,28,43,25));

        jScrollPane1.setBounds(new Rectangle(23,85,493,69));
        jTextField3.setText("此时输入发送信息");
        jTextArea1.setText("");

        panel1.setLayout(null);
        panel1.add(jLabel1);
        panel1.add(jTextField1);
        panel1.add(jLabel2);
        panel1.add(jTextField2);
        panel1.add(jButton1);
        panel1.add(jLabel3);
        panel1.add(jTextField3);
        panel1.add(jButton2);
        panel1.add(jButton3);
        panel1.add(jScrollPane1);
        jScrollPane1.getViewport().add(jTextArea1);

        contentPane.setLayout(null);
        contentPane.add(panel1);
        contentPane.add(panel2);
        panel1.setBounds(0,0,540,160);
        panel2.setBounds(10,160,540,460);

        jButton1.addActionListener(this);
        jButton2.addActionListener(this);
        jButton3.addActionListener(this);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try{
                    sendData("quit|");//向对方发送离开信息
                    socket.close();
                    instr.close();
                    os.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    System.exit(0);
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==jButton1){//连接按钮
            String ip = jTextField1.getText().trim();
            int port = Integer.parseInt(jTextField2.getText().trim());
            connectServer(ip,port);
        }
        if(e.getSource()==jButton2){//发送按钮
            String s = this.jTextField3.getText().trim();
            sendData(s);
        }
        if(e.getSource()==jButton3){//悔棋按钮
            if(canPlay!=true){//该对方走棋
                allChess[x][y]=0;
                panel2.repaint();
                canPlay=true;
                String s = "undo|"+x+"|"+y;
                sendData(s);
                System.out.print("发送悔棋信息");
            }else {//对方已走棋
                message="对方已走棋，不能悔棋了";
                JOptionPane.showMessageDialog(this,message);
                System.out.print("对方已走棋，不能悔棋了");
            }
        }
    }

    private void connectServer(String ip, int port){//连接
        try{
            if(jButton1.getText().trim().equals("连接")){
                jButton1.setText("连接服务器...");
                socket=new Socket(ip,port);
                this.setTitle("你是白方");
                jButton1.setText("正在聊天");
                message = "自己是白棋，黑方先行";
                panel2.repaint();
                MyThread t = new MyThread();
                t.start();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //内部线程类
    class MyThread extends Thread{

        @Override
        public void run(){
            try{
                os=new PrintWriter(socket.getOutputStream());
                instr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while(true){
                    this.sleep(100);
                    if(instr.ready()){
                        String cmd = instr.readLine();
                        jTextArea1.append("服务器端： " + cmd + "\n");
                        //在每个空格字符处进行分解
                        ss=cmd.split("\\|");
                        if(cmd.startsWith("move")){//对方黑子走棋信息
                            int x = Integer.parseInt(ss[1]);
                            int y = Integer.parseInt(ss[2]);
                            allChess[x][y]=1;//黑子落在（x,y）位置
                            message="轮到自己下棋了";
                            panel2.repaint();
                            canPlay=true;
                        }
                        if(cmd.startsWith("undo")){
                            JOptionPane.showMessageDialog(null,"对方撤销上一步棋");
                            int x = Integer.parseInt(ss[1]);
                            int y = Integer.parseInt(ss[2]);
                            allChess[x][y]=0;
                            panel2.repaint();
                            canPlay=false;
                        }
                        if(cmd.startsWith("over")){
                            JOptionPane.showMessageDialog(null,"游戏结束，对方胜！！");
                            panel2.setEnabled(false);
                            canPlay=false;
                        }
                        if(cmd.startsWith("quit")){
                            JOptionPane.showMessageDialog(null,"游戏结束，对方离开了！！");
                            panel2.setEnabled(false);
                            canPlay=false;
                        }
                        if(cmd.startsWith("chat")){
                            jTextArea1.append("服务器端说： "+ss[1]+"\n");
                        }
                    }
                }
            }catch (Exception ex){
                System.out.print("error: "+ex);
            }
        }
    }

    private void sendData(String s){//发送数据
        try{
            os = new PrintWriter(socket.getOutputStream());
            os.println(s);
            os.flush();
            this.jTextArea1.append("Client： "+ s+"\n");
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        Client frm = new Client();
        frm.setVisible(true);
    }

    class GobangPanel extends JPanel {
        BufferedImage bgImage = null;//棋盘背景图片

        GobangPanel() {
            this.addMouseListener(new MouseLis());
            String imagePath = "";
            try {
                imagePath = "GoBangOnline/background2.jpg";//System.getProperty("user.dir") + "/background2.jpg";//TODO 需要图片
                bgImage = ImageIO.read(new File(imagePath.replaceAll("\\\\", "/")));
            } catch (IOException e) {
                //TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            //双缓冲技术防止屏幕闪烁
            BufferedImage bi = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
            Graphics g2 = bi.createGraphics();
            g2.setColor(Color.BLACK);
            //绘制背景
            g2.drawImage(bgImage, 1, 20, this);
            //输出标题信息
            g2.setFont(new Font("黑体", Font.BOLD, 15));
            g2.drawString("游戏信息： " + message, 130, 60);
            //绘制棋盘
            for (int i = 0; i < 19; i++) {
                g2.drawLine(10, 70 + 20 * i, 370, 70 + 20 * i);
                g2.drawLine(10 + 20 * i, 70, 10 + 20 * i, 430);
            }
            //标注点位
            g2.fillOval(68, 128, 6, 6);
            g2.fillOval(308, 128, 6, 6);
            g2.fillOval(308, 368, 6, 6);
            g2.fillOval(68, 368, 6, 6);
            g2.fillOval(308, 248, 6, 6);
            g2.fillOval(188, 128, 6, 6);
            g2.fillOval(68, 248, 6, 6);
            g2.fillOval(188, 368, 6, 6);
            g2.fillOval(188, 248, 6, 6);
            //绘制全部棋子
            for (int i = 0; i < 19; i++) {
                for (int j = 0; j < 19; j++) {
                    if (allChess[i][j] == 1) {//黑子
                        int tempX = i * 20 + 10;
                        int tempY = j * 20 + 70;
                        g2.fillOval(tempX - 7, tempY - 7, 14, 14);
                    } else if (allChess[i][j] == 2) {//白子
                        int tempX = i * 20 + 10;
                        int tempY = j * 20 + 70;
                        g2.setColor(Color.WHITE);
                        g2.fillOval(tempX - 7, tempY - 7, 14, 14);
                        g2.setColor(Color.BLACK);
                        g2.drawOval(tempX - 7, tempY - 7, 14, 14);
                    }
                }
            }
            g.drawImage(bi, 0, 0, this);
        }
    }

    class MouseLis extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (canPlay == true) {
                x = e.getX();
                y = e.getY();
                if (x >= 10 && x < 370 && y >= 70 && y <= 430) {
                    x = x / 20;
                    y = (y - 60) / 20;
                    if (allChess[x][y] == 0) {
                        //判断当前要下的是什么颜色的棋子
                        if (isBlack == true) {
                            allChess[x][y] = 1;
                            //isBlack = false;
                            message = "轮到白方";
                            sendData("move|" + x + "|" + y);//简化了Sting.valueOf
                            canPlay = false;
                            repaint();
                        } else {
                            allChess[x][y] = 2;
                            //isBlack = true;
                            message = "轮到黑方";
                            sendData("move|" + x + "|" + y);
                            canPlay = false;
                            //白子
                            repaint();
                        }
                        //判断这个棋子是否和其他的棋子连成5子，即判断游戏是否结束
                        boolean winFlag = this.checkWin();
                        if (winFlag == true) {
                            message = "游戏结束，" + (allChess[x][y] == 1 ? "黑方":"白方")+"胜";
                            sendData("over|"+message);
                            JOptionPane.showMessageDialog(null,message);
                            System.out.println(message);
                            canPlay = false;
                        }
                    }else{
                        message = "当前位置已经有棋子，请重新落子！";
                        System.out.println(message);
                    }
                }
                repaint();
            } else{
                message = "该对方走棋！";
                JOptionPane.showMessageDialog(null,message);
            }
        }

        private boolean checkWin(){
            int count = 1;//保存共有多少颗相同颜色的棋子相连
            //判断横向是否有5个棋子相连，特点是纵坐标相同
            //即allChess[x][y]中y值相同
            int color = allChess[x][y];
            //通过循环判断棋子是否相连
            //横向的判断
            int i = 1;
            while(color == allChess[x+i][y+0]){
                count++;
                i++;
            }
            i=1;
            while(color==allChess[x-i][y-0]){
                count++;
                i++;
            }
            if(count>=5){
                return true;
            }
            //纵向的判断
            i = 1;
            count = 1;
            while(color == allChess[x+0][y+i]){
                count++;
                i++;
            }
            i=1;
            while(color==allChess[x-0][y-i]){
                count++;
                i++;
            }
            if(count>=5){
                return true;
            }
            //右上+左下斜方向判断
            i = 1;
            count = 1;
            while(color == allChess[x+i][y+i]){
                count++;
                i++;
            }
            i=1;
            while(color==allChess[x-i][y-i]){
                count++;
                i++;
            }
            if(count>=5){
                return true;
            }
            //右下+左上斜方向判断
            i = 1;
            count = 1;
            while(color == allChess[x-i][y+i]){
                count++;
                i++;
            }
            i=1;
            while(color==allChess[x+i][y-i]){
                count++;
                i++;
            }
            if(count>=5){
                return true;
            }
            return false;
        }

    }
}
